package payment.gateway.config.constants;

public interface KafkaConstants {
     String KAFKA_DEBIT_GROUP = "payment-gateway-debit";
     String KAFKA_CREDIT_GROUP = "payment-gateway-credit";
     String KAFKA_LEDGER_GROUP = "payment-gateway-ledger";
     String KAFKA_SETTLE_GROUP = "payment-gateway-settle";
     String KAFKA_FAILURE_GROUP = "payment-gateway-failure";
     String KAFKA_COMPENSATE_GROUP = "payment-gateway-compensate";
     String LISTENER_CONTAINER_FACTORY = "kafkaListenerContainerFactory";

}
