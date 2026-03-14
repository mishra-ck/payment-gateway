package payment.gateway.saga.events;

import payment.gateway.config.constants.Constants;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
