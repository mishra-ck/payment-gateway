package payment.gateway.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String idempotencyKey,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        String currency,
        String status,
        String statusDescription,
        String failureReason,
        String reference,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant updatedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant settledAt
) { }
