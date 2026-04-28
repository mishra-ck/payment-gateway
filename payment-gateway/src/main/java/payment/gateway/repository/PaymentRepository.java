package payment.gateway.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import payment.gateway.domain.model.Payment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    /**  Eager Loading to avoid N+1 on payment history end-point */
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.events WHERE p.id=:id")
    Optional<Payment> findByIdWithEvents(@Param("id") UUID paymentId);

    @Query("SELECT p FROM Payment p WHERE p.statusCode = 'PENDING' " +
            "AND p.createdAt < :cutOff ORDER BY p.createdAT ASC")
    List<Payment> findStalePendingPayments(@Param("cutOff") Instant cutOff);

    @Modifying
    @Query("UPDATE Payment p SET p.statusCode = 'FAILED', p.failureReason = :reason" +
            "WHERE p.id = :id AND p.statusCode NOT in ('SETTLED','FAILED')")
    int forceFailPayment(@Param("id") UUID id , @Param("reason") String reason);
}
