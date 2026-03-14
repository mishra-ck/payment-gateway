package payment.gateway.saga.events;

import payment.gateway.config.constants.Constants;

import java.time.Instant;
import java.util.UUID;

/**
 * TOPIC : payment.failed
 * Published by any service that encounters an unrecoverable error.
 * Consumed by PaymentService (status update) and AccountService (compensate).
 */
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
