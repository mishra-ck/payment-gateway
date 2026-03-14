package payment.gateway.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import payment.gateway.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    /**  Eager Loading to avoid N+1 on payment history end-point */
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.events WHERE p.id=:id")
    Optional<Payment> findByIdWithEvents(@Param("id") UUID paymentId);

}
