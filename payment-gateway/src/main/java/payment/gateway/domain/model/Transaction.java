package payment.gateway.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import payment.gateway.config.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
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

}
