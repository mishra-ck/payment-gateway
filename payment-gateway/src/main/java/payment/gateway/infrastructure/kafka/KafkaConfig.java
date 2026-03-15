package payment.gateway.infrastructure.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * KAFKA Infrastructure configuration
 */
@Configuration
public class KafkaConfig {

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



}
