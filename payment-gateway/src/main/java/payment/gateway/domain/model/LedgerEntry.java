package payment.gateway.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import payment.gateway.config.enums.EntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    private UUID id;
    private UUID paymentId;
    private UUID accountId;
    private EntryType entryType;
    private BigDecimal amount;
    private String currency;
    private String description;
    private boolean reversal = false;
    private UUID reversedEntryId;
    private Instant entryDate;

}
