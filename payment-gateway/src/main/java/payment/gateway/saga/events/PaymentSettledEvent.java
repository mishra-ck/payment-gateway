package payment.gateway.saga.events;

import payment.gateway.config.constants.Constants;

import java.time.Instant;
import java.util.UUID;

public record PaymentSettledEvent(
        UUID paymentId,
        String correlationId,
        UUID journalId,
        Instant occurredAt
) implements SagaEvent {
    @Override
    public String eventType() {
        return Constants.PaymentStatus.SETTLED;
    }
}
