package payment.gateway.saga.events;

import payment.gateway.config.constants.Constants;

import java.time.Instant;
import java.util.UUID;

/**
 * TOPIC : payment.settled
 * Published by LedgerService after writing balanced ledger entries.
 * PaymentService listens to transition payment to SETTLED.
 */
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
