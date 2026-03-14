package payment.gateway.saga.events;

import payment.gateway.config.constants.Constants;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCreditedEvent(
        UUID paymentId,
        String correlationId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        BigDecimal destinationBalanceAfter,
        UUID creditTransactionId,
        Instant occurredAt
) implements SagaEvent {
    @Override
    public String eventType() {
        return Constants.PaymentStatus.CREDITED;
    }
}
