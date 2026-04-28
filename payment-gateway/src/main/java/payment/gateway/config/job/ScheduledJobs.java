package payment.gateway.config.job;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import payment.gateway.repository.IdempotencyRepository;
import payment.gateway.repository.LedgerEntryRepository;
import payment.gateway.repository.PaymentRepository;

import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledJobs {

    private final IdempotencyRepository idempotencyRepository;
    private final PaymentRepository paymentRepository;
    private LedgerEntryRepository ledgerEntryRepository;
    private final MeterRegistry meterRegistry;

    @PersistenceContext
    private EntityManager entityManager;

    // 1. Partition Maintenance - runs at 12:05 AM every month on day 1
    // Stays 2 month ahead
    @Scheduled(cron = "0 5 0 1 * *")
    @Transactional
    public void createNextPartition(){
         var nextNext = YearMonth.now().plusMonths(2);
         var partitionName = "transactions_"+ nextNext.format(DateTimeFormatter.ofPattern("yyyy_MM"));
         var startDate = nextNext.atDay(1);
         var endDate = nextNext.plusMonths(1).atDay(1);

        log.info("PARTITION_MAINTENANCE: creating partition {}", partitionName);

         try{
             entityManager.createNativeQuery(
                     "CREATE TABLE IF NOT EXISTS %s PARTITION OF transactions FOR VALUES FROM ('%s') TO ('%s')"
                             .formatted(partitionName, startDate,endDate)
             ).executeUpdate();
             meterRegistry.counter("partition.created", "name", partitionName).increment();
             log.info("PARTITION_MAINTENANCE_OK: {}", partitionName);

         }catch(Exception e){
             log.error("PARTITION_MAINTENANCE_FAILED: {}", partitionName, e);
             meterRegistry.counter("partition.error").increment();
         }
    }

    // 2. Idempotency Cleanup - every 6 Hrs
    @Scheduled(fixedDelay = 6*3600*1000L, initialDelay = 60_000L)
    @Transactional
    public void cleanupExpiredIdempotencyRecords(){
        var deleted = idempotencyRepository.deleteExpiredRecords(Instant.now());
        if(deleted > 0){
            log.info("IDEMPOTENCY_CLEANUP : deleted {} expired records", deleted);
            meterRegistry.counter("idempotency.records.deleted").increment(deleted);
        }
    }

    // 3. Stale payment recovery
    /**
     * Every 5 minutes - finds payment stuck in PENDING >30 minutes and force-fail them.
     * It handles case where Kafka event was published but the consumer never processed(ex: outage)
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 1_20_000L)
    @Transactional
    public void recoverStalePendingPayments(){
        var cutOff = Instant.now().minus(30, ChronoUnit.MINUTES);
        var stalePayments = paymentRepository.findStalePendingPayments(cutOff);

        if(!stalePayments.isEmpty()){
            log.warn("STALE_PAYMENT_RECOVERY: found {} stale payment", stalePayments.size());

            stalePayments.forEach( payment -> {
                int updated = paymentRepository.forceFailPayment(
                        payment.getId(),
                        "Payment timed out after 30 minutes in PENDING state"
                );
                if(updated > 0){
                   log.warn("STALE_PAYMENT_FAILED: paymentId:{}", payment.getId());
                   meterRegistry.counter("payment.stale.recovered").increment();
                }
            });
        }
    }

    // 4. Ledger Reconciliation
    /**
     * Runs at 2:00 AM every day, verify Ledger is balanced
     * Alert( via metric) if any journal has imbalanced Debit/Credit entries.
     */
    @Transactional(readOnly = true)
    public void reconcileLedger(){
        /*TODO*/
    }
}
