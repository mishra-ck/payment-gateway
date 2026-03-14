package payment.gateway.saga.events;

import payment.gateway.config.constants.Constants;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        String correlationId,
        String failureReason,
        String failedAtStep,
        boolean debitApplied,    // signals whether compensating credit is needed
        Instant occurredAt
) implements SagaEvent {
    @Override
    public String eventType() {
        return Constants.PaymentStatus.FAILED;
    }
}
