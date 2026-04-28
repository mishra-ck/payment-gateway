package payment.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import payment.gateway.domain.model.IdempotencyRecord;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID> {
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**  Cleanup expired records - called by scheduled job */
    @Query("DELETE FROM IdempotencyRecord r where r.expiresAt < :now")
    int deleteExpiredRecords(@Param("now") Instant now);
}
