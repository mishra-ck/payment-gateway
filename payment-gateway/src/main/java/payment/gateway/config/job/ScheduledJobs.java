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

    // Idempotency Cleanup - every 6 Hrs
    @Scheduled(fixedDelay = 6*3600*1000L, initialDelay = 60_000L)
    @Transactional
    public void cleanupExpiredIdempotencyRecords(){
        /*TODO*/
    }
}
