package payment.gateway.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;
import payment.gateway.saga.events.SagaEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * KAFKA Infrastructure configuration
 */
@Configuration
@Slf4j
public class KafkaConfig {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaConfig.class);

    /** -------------- TOPIC NAMES -------------- */
    public static final String TOPIC_PAYMENT_INITIATED   = "payment.initiated";
    public static final String TOPIC_PAYMENT_DEBITED     = "payment.debited";
    public static final String TOPIC_PAYMENT_CREDITED    = "payment.credited";
    public static final String TOPIC_PAYMENT_SETTLED     = "payment.settled";
    public static final String TOPIC_PAYMENT_FAILED      = "payment.failed";
    public static final String TOPIC_COMPENSATE_DEBIT    = "payment.compensate.debit";
    public static final String TOPIC_PAYMENT_DLT         = "payment.dead-letter";

    @Value("${spring.kafka.bootstrap-servers}")
    public String bootstrapServers;
    @Value("spring.kafka.consumer.group-id:payment-gateway")
    public String groupId;

    /** ------------ KAFKA TOPICS Declaration ----------- */
    @Bean
    public NewTopic topicPaymentInitiated(){
        return TopicBuilder.name(TOPIC_PAYMENT_INITIATED)
                .partitions(6).replicas(1).compact().build();
    }
    @Bean
    public NewTopic topicPaymentDebited(){
        return TopicBuilder.name(TOPIC_PAYMENT_DEBITED)
                .partitions(6).replicas(1).build();
    }
    @Bean
    public NewTopic topicPaymentCredited(){
        return TopicBuilder.name(TOPIC_PAYMENT_CREDITED)
                .partitions(6).replicas(1).build();
    }
    @Bean
    public NewTopic topicPaymentSettled(){
        return TopicBuilder.name(TOPIC_PAYMENT_SETTLED)
                .partitions(6).replicas(1).build();
    }
    @Bean
    public NewTopic topicPaymentFailed(){
        return TopicBuilder.name(TOPIC_PAYMENT_FAILED)
                .partitions(6).replicas(1).build();
    }
    @Bean
    public NewTopic topicCompensateDebit(){
        return TopicBuilder.name(TOPIC_COMPENSATE_DEBIT)
                .partitions(6).replicas(1).build();
    }
    @Bean
    public NewTopic topicDeadLetter(){
        return TopicBuilder.name(TOPIC_PAYMENT_DLT)
                .partitions(3).replicas(1).build();
    }

    /** ------------- KAFKA Producer ------------------- */
    @Bean
    public ProducerFactory<String, SagaEvent> producerFactory(){
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Idempotent producer : exactly-one delivery
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,true);
        props.put(ProducerConfig.ACKS_CONFIG,"all");
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,5);
        props.put(ProducerConfig.RETRIES_CONFIG,Integer.MAX_VALUE);

        // Batching for throughput
        props.put(ProducerConfig.BATCH_SIZE_CONFIG,16_000);
        props.put(ProducerConfig.LINGER_MS_CONFIG,5);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");

        return new DefaultKafkaProducerFactory<>(props,new StringSerializer(),
                new JsonSerializer<>());

    }
    @Bean
    public KafkaTemplate<String,SagaEvent> kafkaTemplate(){
        var template = new KafkaTemplate<>(producerFactory());
        template.setObservationEnabled(true);
        return  template;
    }

    /** -------------- KAFKA Consumer ------------------- */
    @Bean
    public ConsumerFactory<String,SagaEvent> consumerFactory(){
        Map<String,Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        //Manual offset commit prevents message loss
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

        // Fetch tuning for throughput
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG,1024);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG,500);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,50);

        var deserializer = new JsonDeserializer<>(SagaEvent.class,objectmapper());
        deserializer.addTrustedPackages("payment.gateway.saga.events");
        deserializer.setUseTypeMapperForKey(false);

        return new DefaultKafkaConsumerFactory<>(props,new StringDeserializer(),deserializer);
    }

    public ConcurrentKafkaListenerContainerFactory<String,SagaEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String,SagaEvent> consumerFactory,
            KafkaTemplate<String,SagaEvent> kafkaTemplate){

        var  factory = new ConcurrentKafkaListenerContainerFactory<String, SagaEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);  // 3 consumer threads per topic partition

        // commit offset only after successful processing
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.MANUAL_IMMEDIATE
        );
        factory.getContainerProperties().setObservationEnabled(true);
        var backOff = new ExponentialBackOff(1000L,2.0);
        backOff.setMaxInterval(16_000L);
        backOff.setMaxElapsedTime(60_000L);

        var errorHandler = new DefaultErrorHandler(
                (record,exception)->{
                    LOG.error("Saga_DLT,Sending to Dead Letter Topic,paymentId:{},error:{}",
                            record.key(),exception.getMessage(),exception);
                    kafkaTemplate.send(TOPIC_PAYMENT_DLT,
                            record.key().toString(),(SagaEvent) record.value());
                }
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }


    private ObjectMapper objectmapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

}
