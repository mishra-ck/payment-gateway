package payment.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import payment.gateway.domain.model.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry,UUID> {
}
