package payment.gateway.saga.events;

import java.time.Instant;
import java.util.UUID;

public sealed interface SagaEvent
    permits PaymentInitiatedEvent, PaymentDebitedEvent,
        PaymentCreditedEvent,PaymentSettledEvent,
        PaymentFailedEvent, CompensateDebitEvent {
    UUID paymentId();
    String correlationId();
    Instant occurredAt();
    String eventType();
}
