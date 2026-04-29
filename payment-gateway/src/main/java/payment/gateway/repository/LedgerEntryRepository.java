package payment.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import payment.gateway.domain.model.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
     List<LedgerEntry> findByPaymentId(UUID paymentId);
     List<LedgerEntry> findByJournalId(UUID journalId);
     Optional<BigDecimal> checkJournalBalance(UUID journalId);

     @Query(value = """
            SELECT journal_id, 
                   SUM(CASE WHEN entry_type = 'DR' THEN amount ELSE 0 END) as total_dr,
                   SUM(CASE WHEN entry_type = 'CR' THEN amount ELSE 0 END) as total_cr
            FROM ledger_entries
            WHERE entry_date BETWEEN :from AND :to
            GROUP BY journal_id
            HAVING ABS(SUM(CASE WHEN entry_type = 'DR' THEN amount ELSE -amount END)) > 0.0001
            """, nativeQuery = true)
    List<Object[]> findImbalancedJournals(@Param("from") Instant yesterday, @Param("to") Instant now);
}
