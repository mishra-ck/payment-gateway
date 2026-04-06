package payment.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.gateway.domain.model.LedgerEntry;
import payment.gateway.exception.LedgerImbalanceException;
import payment.gateway.infrastructure.kafka.KafkaConfig;
import payment.gateway.repository.LedgerEntryRepository;
import payment.gateway.saga.events.PaymentCreditedEvent;
import payment.gateway.saga.events.PaymentSettledEvent;
import payment.gateway.saga.events.SagaEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class LedgerService {
    private final static Logger LOG = LoggerFactory.getLogger(LedgerService.class);
    private final LedgerEntryRepository ledgerEntryRepository;
    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository, KafkaTemplate<String, SagaEvent> kafkaTemplate) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void recordPaymentLedgerEntries(PaymentCreditedEvent event) {
        LOG.info("LEDGER_RECORD_START : paymentId:{},amount:{} {}",
                event.paymentId(), event.amount(),event.currency());

        UUID journalId = UUID.randomUUID();

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

        // verify balance before persist
        assertBalanced(journalId,debitEntry,creditEntry);

        ledgerEntryRepository.saveAll(List.of(debitEntry,creditEntry));

        LOG.info("LEDGER_RECORD_OK : paymentId:{}, journalId:{}, amount:{}",
                event.paymentId(), journalId, event.amount());

        //Publish settled event - payment completes
        kafkaTemplate.send(
                KafkaConfig.TOPIC_PAYMENT_SETTLED,
                event.paymentId().toString(),
                new PaymentSettledEvent(
                        event.paymentId(),
                        event.correlationId(),
                        journalId,
                        Instant.now()
                )
        );

    }
    // Reverse Ledger entries for failed payments
    @Transactional
    public void reverseLedgerEntries(UUID paymentId, String correlationId) {

        var entries = ledgerEntryRepository.findByPaymentId(paymentId);
        if(entries.isEmpty()){
            LOG.info("LEDGER_REVERSE_SKIPPED : no entries found for paymentId={}",paymentId);
            return;
        }

        UUID reverseJournalId = UUID.randomUUID();
        var reversals = entries.stream()
                .filter(e -> !e.isReversal())
                .map(e -> e.reverse(reverseJournalId))
                .toList();
        if(!reversals.isEmpty()){
            ledgerEntryRepository.saveAll(reversals);
            LOG.warn("LEDGER_REVERSED : paymentId={},reverseJournalId={},count={}",
                    paymentId,reverseJournalId,reversals.size());
        }
    }

    private void assertBalanced(UUID journalId, LedgerEntry debit, LedgerEntry credit){
        if(debit.getAmount().compareTo(credit.getAmount()) != 0){
            throw new LedgerImbalanceException("CRITICAL: Ledger imbalance for journalId=%s: DR=%s, CR=%s"
                    .formatted(journalId, debit.getAmount(), credit.getAmount()));
        }
    }

}
