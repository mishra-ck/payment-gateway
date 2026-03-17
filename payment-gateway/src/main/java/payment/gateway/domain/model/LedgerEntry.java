package payment.gateway.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import payment.gateway.config.enums.EntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LedgerEntry {

    @Id
    private UUID id;
    private UUID journalId;
    private UUID paymentId;
    private UUID accountId;
    private EntryType entryType;
    private BigDecimal amount;
    private String currency;
    private String description;
    private boolean reversal = false;
    private UUID reversedEntryId;
    private Instant entryDate;

    public static LedgerEntry debitEntry(UUID journalId, UUID paymentId, UUID accountId,
        BigDecimal amount, String currency, String description){
        return LedgerEntry.builder()
                .journalId(journalId)
                .paymentId(paymentId)
                .accountId(accountId)
                .entryType(EntryType.DR)
                .amount(amount)
                .currency(currency)
                .description(description)
                .build();
    }
    public static LedgerEntry creditEntry(UUID journalId, UUID paymentId, UUID accountId,
                                         BigDecimal amount, String currency, String description){
        return LedgerEntry.builder()
                .journalId(journalId)
                .paymentId(paymentId)
                .accountId(accountId)
                .entryType(EntryType.CR)
                .amount(amount)
                .currency(currency)
                .description(description)
                .build();
    }

}
