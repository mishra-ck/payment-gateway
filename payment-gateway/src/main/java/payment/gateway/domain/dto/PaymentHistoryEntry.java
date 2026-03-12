package payment.gateway.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;
public record PaymentHistoryEntry(UUID eventId,
      String eventType,
      String fromStatus,
      String toStatus,
      String actor,
      String detail,
      String correlationId,
      @JsonFormat(shape = JsonFormat.Shape.STRING)
      Instant occurredAt
) {}
