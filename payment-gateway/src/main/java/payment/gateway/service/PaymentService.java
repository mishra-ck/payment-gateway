package payment.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.gateway.config.constants.Constants;
import payment.gateway.domain.dto.PaymentHistoryResponse;
import payment.gateway.domain.dto.PaymentRequest;
import payment.gateway.domain.dto.PaymentResponse;
import payment.gateway.domain.model.IdempotencyRecord;
import payment.gateway.domain.model.Payment;
import payment.gateway.domain.model.PaymentEvent;
import payment.gateway.domain.model.PaymentStatus;
import payment.gateway.exception.InvalidPaymentException;
import payment.gateway.exception.PaymentNotFoundException;
import payment.gateway.infrastructure.kafka.KafkaConfig;
import payment.gateway.repository.AccountRepository;
import payment.gateway.repository.IdempotencyRepository;
import payment.gateway.repository.PaymentRepository;
import payment.gateway.saga.events.PaymentInitiatedEvent;

import java.io.IOException;
import java.time.Instant;
import java.util.InvalidPropertiesFormatException;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository ;
    private final IdempotencyRepository idempotencyRepository;
    private final AccountRepository accountRepository;
    private final Counter paymentDuped;
    private final Counter paymentsInitiated;
    private final Timer initiationTimer;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate kafkaTemplate;

    public PaymentService(PaymentRepository paymentRepository, IdempotencyRepository idempotencyRepository,
                          AccountRepository accountRepository, MeterRegistry meterRegistry,
                          ObjectMapper objectMapper, KafkaTemplate kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.accountRepository = accountRepository;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.paymentDuped = Counter.builder("payment.duplicated")
                .description("Payment returned from idempotency cache").register(this.meterRegistry);
        this.paymentsInitiated = Counter.builder("payment-initiated")
                .description("Total payment initiated").register(meterRegistry);
        this.initiationTimer = Timer.builder("payment.initiation.duration")
                .description("Time to initiate a payment").register(meterRegistry);
    }

    /** -------- Initiate Payment --------- */
    @Transactional
    @CircuitBreaker(name = "payment-service",fallbackMethod = "initiatePaymentFallback")
    @RateLimiter(name = "payment-api",fallbackMethod = "rateLimitFallback")
    @Observed(name = "payment.initiate",contextualName = "initiatePayment")
    public PaymentResponse initiatePayment(PaymentRequest request, String idempotencyKey) {
        return initiationTimer.record(() -> doInitiatePayment(request,idempotencyKey));
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

            if(!sourceAccount.getCurrency().equals(request.currency())){
                throw new InvalidPropertiesFormatException("Currency mismatch between source and target accounts");
            }

            /** STEP-3 ------ Create Payment ---------- **/
            var payment = Payment.builder()
                    .idempotencyKey(idempotencyKey)
                    .sourceAccountId(request.sourceAccountId())
                    .targetAccountId(request.targetAccountId())
                    .amount(request.amount())
                    .currency(request.currency())
                    .statusCode(Constants.PaymentStatus.PENDING)
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

            /** Step-4 --------- Publish to Kafka event -------------- */
            var event = new PaymentInitiatedEvent(
                    payment.getId(),
                    idempotencyKey,
                    request.sourceAccountId(),
                    request.targetAccountId(),
                    request.amount(),
                    request.currency(),
                    Instant.now()
            );
            kafkaTemplate.send(
                    KafkaConfig.TOPIC_PAYMENT_INITIATED,  // topic name
                    payment.getId().toString(),  // partition key - routes same payment to same partition
                    event   // data
            ).whenComplete((result, ex) -> {
                if (ex != null) {  // exception occurred
                    LOG.error("KAFKA_PUBLISH_FAILED : paymentId={}", payment.getId(), ex);
                    // This payment needs to re-tried again
                }
            });

            /** STEP-5 ---------- Save Idempotency Record  ------------*/
            var response = toPaymentResponse(payment);
            saveIdempotencyRecord(idempotencyKey,payment.getId(),201,response);

            paymentsInitiated.increment();

            log.info("Payment-Initiated, paymentId:{},sourceAccount:{}",payment.getId(),payment.getSourceAccountId());
            return  response;

        } catch (InvalidPropertiesFormatException e) {
            throw new RuntimeException(e);
        } finally {
            MDC.remove("idempotencyKey");
            MDC.remove("paymentId");
        }
    }

    private void saveIdempotencyRecord(String idempotencyKey, UUID id, int status, PaymentResponse response) {
        try {
            var record = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .paymentId(id)
                    .responseStatus(status)
                    .responseBody(objectMapper.writeValueAsString(response))
                            .build();
            // save the updated record
            idempotencyRepository.save(record);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize idempotency response", e);
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentDetails(UUID paymentId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: "+ paymentId));
        return toPaymentResponse(payment);
    }


    public PaymentHistoryResponse getPaymentHistory(UUID paymentId) {
        var paymentHistory = paymentRepository.findByIdWithEvents(paymentId)
                .orElseThrow();
        return null;
    }

    private <T> T deserialize(String json,Class<T> type){
        try{
            return objectMapper.readValue(json, type);
        }catch(IOException e){
            throw new RuntimeException("Failed to de-serialized response",e);
        }
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        var status = payment.getStatus();
        return new PaymentResponse(
                payment.getId(),
                payment.getIdempotencyKey(),
                payment.getSourceAccountId(),
                payment.getTargetAccountId(),
                payment.getAmount(),
                payment.getCurrency(),
                status.code(),
                status.description(),
                payment.getFailureReason(),
                payment.getReference(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getSettledAt()
        );
    }

    @Transactional
    public void transitionPaymentStatus(UUID paymentId, PaymentStatus newStatus,
                                        String correlationId, String detail) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found : "+ paymentId));

        var oldStatus = payment.getStatusCode();
        payment.transitionTo(newStatus);

        var event = PaymentEvent.builder()
                .eventType("STATUS_TRANSITION")
                .fromStatus(oldStatus)
                .toStatus(newStatus.code())
                .updatedBy("saga")
                .detail(detail)
                .correlationId(correlationId)
                .build();

        payment.addEvent(event);

        paymentRepository.save(payment);
        LOG.info("PAYMENT_STATUS_TRANSITION : paymentId={},{} -> {}",paymentId,oldStatus,newStatus.code());
    }
}
