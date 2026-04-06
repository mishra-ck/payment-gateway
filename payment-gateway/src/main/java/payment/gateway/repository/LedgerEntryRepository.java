package payment.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import payment.gateway.domain.model.LedgerEntry;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
     List<LedgerEntry> findByPaymentId(UUID paymentId);
    /*TODO*/
}
