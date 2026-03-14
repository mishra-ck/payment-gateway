package payment.gateway.saga.events;

import payment.gateway.config.constants.Constants;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * TOPIC : payment.compensate.debit
 * Compensating event — instructs AccountService to reverse a previously
 * applied debit. Published when credit step fails after debit succeeded.
 */
public record CompensateDebitEvent(
        UUID paymentId,
        String correlationId,
        UUID sourceAccountId,
        BigDecimal amount,
        String currency,
        String reason,
        Instant occurredAt
) implements SagaEvent {
    @Override
    public String eventType() {
        return Constants.PaymentStatus.COMPENSATED_DEBIT;
    }
}
