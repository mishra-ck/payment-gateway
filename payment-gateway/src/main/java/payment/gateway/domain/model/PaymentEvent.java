package payment.gateway.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event log, provide full audit trail. Every change is recorded, never updated only appended
 */
@Entity
@Table(name = "payment_events",
    indexes = {
        @Index(name = "idx_event_payment_id",columnList = "payment_id"),
            @Index(name = "idx_event_occurred_at",columnList = "occurred_at")
    }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id",updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id",nullable = false)
    private Payment payment;

    @Column(name = "event_type",nullable = false, length = 50)
    private String eventType;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    @Column(name = "detail", columnDefinition = "text")
    private String detail;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

}
