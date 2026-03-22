package payment.gateway.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import payment.gateway.exception.InsufficientFundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_account_number", columnList = "account_number", unique = true),
                @Index(name = "idx_accounts_owner_id",       columnList = "owner_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id",updatable = false,nullable = false)
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 34)
    private String accountNumber;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Available balance after pending holds */
    @Column(name = "available_balance", nullable = false, precision = 15, scale = 4)
    private BigDecimal availableBalance;

    /** Actual ledger balance (includes pending debits) */
    @Column(name = "ledger_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal ledgerBalance;

    /** Reserved/held funds not yet settled */
    @Column(name = "held_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal heldAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    /** Debit available balance  */
    public void debit(BigDecimal amount){
        validateActive();
        if(availableBalance.compareTo(amount) < 0){
           throw new InsufficientFundException("Insufficient Funds :  available=%.4f, requested=%.4f, account=%s"
                   .formatted(availableBalance, amount, accountNumber));
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        this.ledgerBalance = this.ledgerBalance.subtract(amount);
    }

    /** Credit available balance  */
    public void credit(BigDecimal amount){
        validateActive();
        this.availableBalance = this.availableBalance.add(amount);
        this.ledgerBalance = this.ledgerBalance.add(amount);
    }
    private void validateActive(){
        if(status != AccountStatus.ACTIVE){
            throw new IllegalStateException("Account %s not active : %s".formatted(accountNumber,status));
        }
    }

    public enum AccountStatus { ACTIVE, SUSPENDED, CLOSED }

}
