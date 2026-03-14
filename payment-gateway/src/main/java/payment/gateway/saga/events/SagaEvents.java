package payment.gateway.saga.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public final class SagaEvents {
    private SagaEvents() {}
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,property = "eventType")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = PaymentInitiatedEvent.class,name = "PAYMENT_INITIATED"),
        @JsonSubTypes.Type(value = PaymentDebitedEvent.class,   name = "PAYMENT_DEBITED"),
        @JsonSubTypes.Type(value = PaymentCreditedEvent.class,  name = "PAYMENT_CREDITED"),
        @JsonSubTypes.Type(value = PaymentSettledEvent.class,   name = "PAYMENT_SETTLED"),
        @JsonSubTypes.Type(value = PaymentFailedEvent.class,    name = "PAYMENT_FAILED"),
        @JsonSubTypes.Type(value = CompensateDebitEvent.class,  name = "COMPENSATE_DEBIT")
    })
    SagaEvent event;

}
