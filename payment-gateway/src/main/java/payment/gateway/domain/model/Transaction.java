package payment.gateway.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import payment.gateway.config.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Transaction {

    @Id
    private UUID id;
    private UUID paymentId;
    private UUID accountId;
    private TransactionType type;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceAfter;
    private String description;
    private String reference;
    private Instant occurredAt;
    private String correlationId;

    public static Transaction debit(UUID paymentId,UUID accountId, BigDecimal amount,
        String currency,BigDecimal balanceAfter,String correlationId ){
        return Transaction.builder()
        .paymentId(paymentId)
                .accountId(accountId)
                .type(TransactionType.DEBIT)
                .currency(currency)
                .balanceAfter(balanceAfter)
                .description("Payment Debit")
                .correlationId(correlationId)
                .build();
    }
    public static Transaction credit(UUID paymentId,UUID accountId, BigDecimal amount,
                                    String currency,BigDecimal balanceAfter,String correlationId ){
        return Transaction.builder()
                .paymentId(paymentId)
                .accountId(accountId)
                .type(TransactionType.CREDIT)
                .currency(currency)
                .balanceAfter(balanceAfter)
                .description("Payment Credit")
                .correlationId(correlationId)
                .build();
    }

}
