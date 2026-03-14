package payment.gateway.saga.events;

import payment.gateway.config.constants.Constants;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentInitiatedEvent(
        UUID paymentId,
        String correlationId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) implements  SagaEvent{
    @Override
    public String eventType() {
        return Constants.PaymentStatus.INITIATED;
    }
}
