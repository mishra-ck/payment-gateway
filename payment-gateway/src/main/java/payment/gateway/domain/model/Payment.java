package payment.gateway.domain.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_payments_idempotency_key", columnList = "idempotency_key", unique = true),
                @Index(name = "idx_payments_source_account",  columnList = "source_account_id"),
                @Index(name = "idx_payments_status",          columnList = "status_code"),
                @Index(name = "idx_payments_created_at",      columnList = "created_at")
        }
)
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "transactions")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "target_account_id", nullable = false)
    private UUID targetAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "failure_step", length = 20)
    private String failureStep;

    @Column(name = "reference", length = 256)
    private String reference;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @OneToMany(mappedBy = "payment",cascade =CascadeType.ALL,fetch = FetchType.LAZY)
    @OrderBy("occurredAt ASC")
    @Builder.Default
    private List<PaymentEvent> events = new ArrayList<>();

    public void addEvent(PaymentEvent event) {
        events.add(event);
        event.setPayment(this);
    }

}
