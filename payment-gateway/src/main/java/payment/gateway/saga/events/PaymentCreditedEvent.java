package payment.gateway.saga.events;

import payment.gateway.config.constants.Constants;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * TOPIC : payment.credited
 * Published by AccountService after successfully crediting destination.
 * Consumed by LedgerService to record the double-entry ledger entries.
 */
public record PaymentCreditedEvent(
        UUID paymentId,
        String correlationId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        String currency,
        BigDecimal targetAccBalanceAfter,
        UUID creditTransactionId,
        Instant occurredAt
) implements SagaEvent {
    @Override
    public String eventType() {
        return Constants.PaymentStatus.CREDITED;
    }
}
