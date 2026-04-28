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

    // Partition Maintenance - runs at 12:05 AM every month on day 1
    @Scheduled(cron = "0 5 0 1 * *")
    @Transactional
    public void createNextPartition(){
        /*TODO*/
    }

    // Idempotency Cleanup - every 6 Hrs
    @Scheduled(fixedDelay = 6*3600*1000L, initialDelay = 60_000L)
    @Transactional
    public void cleanupExpiredIdempotencyRecords(){
        var deleted = idempotencyRepository.deleteExpiredRecords(Instant.now());
        if(deleted > 0){
            log.info("IDEMPOTENCY_CLEANUP : deleted {} expired records", deleted);
            meterRegistry.counter("idempotency.records.deleted").increment(deleted);
        }
    }

    @Transactional
    public void recoverStalePendingPayments(){
        /*TODO*/
    }

}
