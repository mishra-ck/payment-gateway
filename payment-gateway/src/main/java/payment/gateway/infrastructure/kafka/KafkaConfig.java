package payment.gateway.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

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

}
