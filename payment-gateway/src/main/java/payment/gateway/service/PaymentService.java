package payment.gateway.service;

import com.fasterxml.jackson.databind.ObjectReader;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import payment.gateway.config.constants.Constants;
import payment.gateway.domain.dto.PaymentRequest;
import payment.gateway.domain.dto.PaymentResponse;
import payment.gateway.domain.model.Payment;
import payment.gateway.domain.model.PaymentEvent;
import payment.gateway.exception.InvalidPaymentException;
import payment.gateway.repository.AccountRepository;
import payment.gateway.repository.IdempotencyRepository;
import payment.gateway.repository.PaymentRepository;

import java.io.IOException;

@Service
@Slf4j
public class PaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository ;
    private final IdempotencyRepository idempotencyRepository;
    private final AccountRepository accountRepository;
    private final Counter paymentDuped;
    private final MeterRegistry meterRegistry;
    private final ObjectReader objectMapper;

    public PaymentService(PaymentRepository paymentRepository, IdempotencyRepository idempotencyRepository, Counter paymentDeduped, AccountRepository accountRepository, MeterRegistry meterRegistry, ObjectReader objectMapper) {
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.accountRepository = accountRepository;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
        this.paymentDuped = Counter.builder("payment.duplicated")
                .description("Payment returned from idempotency cache").register(this.meterRegistry);
    }

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request, String idempotencyKey) {
        /* TBD - rate limiter need to be added */
        return doInitiatePayment(request,idempotencyKey);
    }
    private PaymentResponse doInitiatePayment(PaymentRequest request, String idempotencyKey){
        MDC.put("idempotencyKey",idempotencyKey);

        try{

            /** STEP-1 ------ Idempotency Check ---------- **/
            var existing = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
            if(existing.isPresent() && !existing.get().isExpired()){
                LOG.info("Idempotency Hit : key{}",idempotencyKey);
                paymentDuped.increment();
                return deserialize(existing.get().getResponseBody(),PaymentResponse.class);
            }
            /** STEP-2 ------ Validate Accounts ---------- **/
            var sourceAccount = accountRepository.findById(request.sourceAccountId())
                    .orElseThrow(
                            () -> new InvalidPaymentException("Source Account not found: "+ request.sourceAccountId())
                    );
            var targetAccount = accountRepository.findById(request.targetAccountId())
                    .orElseThrow(
                            () -> new InvalidPaymentException("Target Account not found: "+ request.targetAccountId())
                    );
            /*  TBC - Currency validation of source and target accounts */

            /** STEP-3 ------ Create Payment ---------- **/
            var payment = Payment.builder()
                    .idempotencyKey(idempotencyKey)
                    .sourceAccountId(request.sourceAccountId())
                    .targetAccountId(request.targetAccountId())
                    .amount(request.amount())
                    .currency(request.currency())
                    .statusCode("Pending")
                    .reference(request.reference())
                    .build();

            paymentRepository.save(payment);

            // Append initial payment event to audit trail
            var initEvent = PaymentEvent.builder()
                    .eventType(Constants.PaymentStatus.INITIATED)
                    .fromStatus(null)
                    .toStatus(Constants.PaymentStatus.PENDING)
                    .updatedBy(Constants.SYSTEM)
                    .correlationId(idempotencyKey)
                    .build();
            payment.addEvent(initEvent);
            paymentRepository.save(payment);

            MDC.put("paymentId",payment.getId().toString());

            /* TBD */

        }finally {
            MDC.remove("idempotencyKey");
            MDC.remove("paymentId");
        }

        return null;
    }
    private <T> T deserialize(String json,Class<T> type){
        try{
            return objectMapper.readValue(json, type);
        }catch(IOException e){
            throw new RuntimeException("Failed to de-serialized response",e);
        }
    }
}
