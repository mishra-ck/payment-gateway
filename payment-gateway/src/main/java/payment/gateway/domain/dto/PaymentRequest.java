package payment.gateway.domain.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record PaymentRequest(
        @NotNull(message = "Source Account Id is required")
        UUID sourceAccountId,
        @NotNull(message = "Target Account Id is required")
        UUID targetAccount,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.1",message = "Amount should be more than 0")
        @Digits(integer = 15, fraction = 4, message = "Amount exceeds precision limits")
        BigDecimal amount,
        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3,message = "Currency must be of 3 chars")
        @Pattern(regexp = "[A-Z]{3}",message = "Currency must be upper case")
        String currency,
        @Size(max = 256,message = "Reference too long")
        String reference,
        Map<String,String> metaData
) { }
