package payment.gateway.domain.dto;

import java.util.List;
import java.util.UUID;

public record PaymentHistoryResponse(
        UUID paymentId,
        String currentStatus,
        List<PaymentHistoryEntry> history
) { }
