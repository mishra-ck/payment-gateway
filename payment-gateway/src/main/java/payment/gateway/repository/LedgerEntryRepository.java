package payment.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import payment.gateway.domain.model.LedgerEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
     List<LedgerEntry> findByPaymentId(UUID paymentId);
     List<LedgerEntry> findByJournalId(UUID journalId);
     Optional<BigDecimal> checkJournalBalance(UUID journalId);
}
