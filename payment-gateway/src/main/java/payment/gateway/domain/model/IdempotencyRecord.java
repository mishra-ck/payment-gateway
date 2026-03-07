package payment.gateway.domain.model;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false,nullable = false)
    private UUID id ;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;
    @Column(name = "payment_id")
    private UUID paymentId;
    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", columnDefinition = "text", nullable = false)
    private String responseBody;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public void setExpiry(){
        if(expiresAt == null){
            expiresAt = Instant.now().plusSeconds(864000); // 24 Hrs TTL
        }
    }
    public boolean isExpired(){
        return Instant.now().isAfter(expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
