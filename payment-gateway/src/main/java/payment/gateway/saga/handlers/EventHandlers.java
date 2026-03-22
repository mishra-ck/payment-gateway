package payment.gateway.saga.handlers;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import payment.gateway.config.constants.Constants;
import payment.gateway.config.constants.KafkaConstants;
import payment.gateway.domain.model.PaymentStatus;
import payment.gateway.infrastructure.kafka.KafkaConfig;
import payment.gateway.saga.events.PaymentInitiatedEvent;
import payment.gateway.saga.events.SagaEvent;
import payment.gateway.service.AccountService;
import payment.gateway.service.LedgerService;
import payment.gateway.service.PaymentService;

import java.util.UUID;

/**
 * SAGA Choreography event handlers
 * KAFKA Listener are configured in this handler.
 * This handler has the responsibility of delegating request to service layer.
 * Sets up MDC context for logging/tracing purpose.
 */
@Component
@Slf4j
public class EventHandlers {
    private static final Logger LOG = LoggerFactory.getLogger(EventHandlers.class);
    private final AccountService accountService;
    private final LedgerService ledgerService;
    private final PaymentService paymentService;
    private final IdempotencyHandlers idempotencyHandlers;
    private final MeterRegistry meterRegistry;

    public EventHandlers(AccountService accountService, LedgerService ledgerService,
                         PaymentService paymentService, IdempotencyHandlers idempotencyHandlers,
                         MeterRegistry meterRegistry) {
        this.accountService = accountService;
        this.ledgerService = ledgerService;
        this.paymentService = paymentService;
        this.idempotencyHandlers = idempotencyHandlers;
        this.meterRegistry = meterRegistry;
    }

    /** payment.initiated  --> debit source account */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_PAYMENT_INITIATED,
            groupId = KafkaConstants.KAFKA_DEBIT_GROUP,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void onPaymentInitiated(ConsumerRecord<String, SagaEvent> record, Acknowledgment ack){

        var event = (PaymentInitiatedEvent)record.value();
        var sagaKey = "debit:"+event.paymentId();

        moveMDC(event.paymentId(),event.correlationId(), "DEBIT", () ->{
            if(idempotencyHandlers.alredyProcessed(sagaKey)){
                LOG.info("SAGA_DEBIT_SKIP: already processed, sagaKey:{}",sagaKey);
                ack.acknowledge();
                return;
            }
            try{
                accountService.processDebit(event);
                idempotencyHandlers.markProcessed(sagaKey);
                meterRegistry.counter("saga.event.processed","event", Constants.PaymentStatus.INITIATED).increment();
                ack.acknowledge();
                LOG.info("SAGA_DEBIT_DONE,paymentId:{}",event.paymentId());
            }catch (Exception e){
                LOG.error("SAGA_DEBIT_FAILED, paymentId:{},error:{}",event.paymentId(),e.getMessage(),e);
                meterRegistry.counter("saga.event.failed","event",Constants.PaymentStatus.INITIATED).increment();
                /*TBD -  ack in fail case */
                throw e;
            }
        });
    }

    /** payment.debited --> credit target account */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_PAYMENT_DEBITED,
            groupId = KafkaConstants.KAFKA_CREDIT_GROUP,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void onPaymentDebited(ConsumerRecord<String, SagaEvent> record, Acknowledgment ack){

        var event = (PaymentInitiatedEvent)record.value();
        var sagaKey = "credit:"+event.paymentId();

        moveMDC(event.paymentId(), event.correlationId(), "CREDIT",()->{
           if(idempotencyHandlers.alredyProcessed(sagaKey)){
               LOG.info("SAGA_CREDIT_SKIP: already processed");
               ack.acknowledge();
               return;
           }
           // Moving to PROCESSING
            paymentService.transitionPaymentStatus(
              event.paymentId(),
                    PaymentStatus.processing(),
                    event.correlationId(),
                    "Debit applied, crediting target account"
            );

           try{
               paymentService.processCredit(event);
               idempotencyHandlers.markProcessed(sagaKey);
               meterRegistry.counter("saga.event.processed","event",Constants.PaymentStatus.DEBITED).increment();
               ack.acknowledge();
           }catch (Exception ex){
               LOG.info("SAGA_CREDIT_ERROR: paymentId:{}", event.paymentId(),ex);
               meterRegistry.counter("saga.event.failed","event",Constants.PaymentStatus.INITIATED).increment();
           }
        });
    }
    @KafkaListener(
            topics = KafkaConfig.TOPIC_PAYMENT_CREDITED,
            groupId = KafkaConstants.KAFKA_LEDGER_GROUP,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void onPaymentCredited(){
        /*TBD*/
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_PAYMENT_SETTLED,
            groupId = KafkaConstants.KAFKA_SETTLE_GROUP,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void onPaymentSettled(){
        /*TBD*/
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_PAYMENT_FAILED,
            groupId = KafkaConstants.KAFKA_FAILURE_GROUP,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void onPaymentFailed(){
        /*TBD*/
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_COMPENSATE_DEBIT,
            groupId = KafkaConstants.KAFKA_COMPENSATE_GROUP,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void onCompensationDebit(){
        /*TBD*/
    }

    private void moveMDC(UUID paymentId, String correlationId, String step, Runnable action){
        MDC.put("paymentId",paymentId.toString());
        MDC.put("correlationId",correlationId);
        MDC.put("sagaStep",step);
        try{
            action.run();  // separate thread created
        }finally {
            MDC.remove("paymentId");
            MDC.remove("correlationId");
            MDC.remove("sagaStep");
        }
    }
}
