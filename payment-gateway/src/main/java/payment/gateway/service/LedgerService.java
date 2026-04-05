package payment.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.gateway.domain.model.LedgerEntry;
import payment.gateway.saga.events.PaymentCreditedEvent;

import java.util.UUID;

@Service
@Slf4j
public class LedgerService {
    private final static Logger LOG = LoggerFactory.getLogger(LedgerService.class);
    @Transactional
    public void recordPaymentLedgerEntries(PaymentCreditedEvent event) {
        LOG.info("LEDGER_RECORD_START : paymentId:{},amount:{} {}",
                event.paymentId(), event.amount(),event.currency());

        UUID journalId =UUID.randomUUID();

        // Debit Source account
        var debitEntry = LedgerEntry.debitEntry(
                journalId,
                event.paymentId(),
                event.sourceAccountId(),
                event.amount(),
                event.currency(),
                "Payment sent to "+ event.targetAccountId()
                );

        // Credit Target account
        var creditEntry = LedgerEntry.creditEntry(
                journalId,
                event.paymentId(),
                event.targetAccountId(),
                event.amount(),
                event.currency(),
                "Payment received from "+ event.sourceAccountId()
        );
    }
    public void reverseLedgerEntries(UUID uuid, String s) {
        /*TODO*/
    }
}
