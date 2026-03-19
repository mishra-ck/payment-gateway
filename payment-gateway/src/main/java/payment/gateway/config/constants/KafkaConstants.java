package payment.gateway.config.constants;

public interface KafkaConstants {
    public static final String KAFKA_DEBIT_GROUP = "payment-gateway-debit";
    public static final String KAFKA_CREDIT_GROUP = "payment-gateway-credit";
    public static final String LISTENER_CONTAINER_FACTORY = "kafkaListenerContainerFactory";

}
