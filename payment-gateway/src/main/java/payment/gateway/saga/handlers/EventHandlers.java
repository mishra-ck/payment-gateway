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
import payment.gateway.config.enums.SagaStep;
import payment.gateway.domain.model.PaymentStatus;
import payment.gateway.infrastructure.kafka.KafkaConfig;
import payment.gateway.saga.events.*;
import payment.gateway.service.AccountService;
import payment.gateway.service.LedgerService;
import payment.gateway.service.PaymentService;

import java.util.UUID;

/**
 * SAGA Choreography event handlers
 * KAFKA Listener are configured in this handler.
 * This handler has the responsibility of delegating request to service layer.
 * Sets up MDC context for logging/tracing purpose.
 *
 * PaymentId- used as Kafka Message Key
 * Therefore same payment will always go to same partition
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
            if(idempotencyHandlers.alreadyProcessed(sagaKey)){
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

        var event = (PaymentDebitedEvent) record.value();
        var sagaKey = "credit:"+event.paymentId();

        moveMDC(event.paymentId(), event.correlationId(), "CREDIT",()->{
           if(idempotencyHandlers.alreadyProcessed(sagaKey)){
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
               accountService.processCredit(event);
               idempotencyHandlers.markProcessed(sagaKey);
               meterRegistry.counter("saga.event.processed","event",Constants.PaymentStatus.DEBITED).increment();
               ack.acknowledge();
           }catch (Exception ex){
               LOG.info("SAGA_CREDIT_ERROR: paymentId:{}", event.paymentId(),ex);
               meterRegistry.counter("saga.event.failed","event",Constants.PaymentStatus.INITIATED).increment();
           }
        });
    }

    /** payment.credited --> write the ledger entries */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_PAYMENT_CREDITED,
            groupId = KafkaConstants.KAFKA_LEDGER_GROUP,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void onPaymentCredited(ConsumerRecord<String,SagaEvent> record, Acknowledgment ack){
        var event = (PaymentCreditedEvent)record.value();
        var sagaKey = "ledger"+ event.paymentId();

        moveMDC(event.paymentId(), event.correlationId(),"LEDGER",() ->{
            if(idempotencyHandlers.alreadyProcessed(sagaKey)){
                ack.acknowledge();
                return;
            }
            try{
                ledgerService.recordPaymentLedgerEntries(event);
                idempotencyHandlers.markProcessed(sagaKey);
                meterRegistry.counter("saga.event.processed","event", Constants.PaymentStatus.CREDITED).increment();
                ack.acknowledge();
            }catch (Exception ex){
                LOG.error("SAGA_LEDGER_ERROR : paymentId={}",event.paymentId(),ex);
                meterRegistry.counter("saga.event.failed","event","PAYMENT_CREDITED").increment();
                throw ex ;
            }
        });
    }
    /** payment.settled  --> mark payment settled */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_PAYMENT_SETTLED,
            groupId = KafkaConstants.KAFKA_SETTLE_GROUP,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void onPaymentSettled(ConsumerRecord<String,SagaEvent> record, Acknowledgment ack){
        var event = (PaymentSettledEvent)record.value();

        moveMDC(event.paymentId(), event.correlationId(), "SETTLED",() ->{
            paymentService.transitionPaymentStatus(
                    event.paymentId(),
                    PaymentStatus.settled(),
                    event.correlationId(),
                    "Ledger entries recorded, journalId="+event.journalId()
            );

            meterRegistry.counter("saga.event.processed", "event",Constants.PaymentStatus.SETTLED).increment();
            ack.acknowledge();
            LOG.info("SAGA_SETTLED : paymentId={}",event.paymentId());
        });
    }

    /** payment.failed  --> mark FAILED, publish compensate if debit already done */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_PAYMENT_FAILED,
            groupId = KafkaConstants.KAFKA_FAILURE_GROUP,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void onPaymentFailed(ConsumerRecord<String,SagaEvent> record, Acknowledgment ack){
        var event = (PaymentFailedEvent)record.value();

        moveMDC(event.paymentId(), event.correlationId(), "FAIL",()->{
            var step = SagaStep.valueOf(event.failedAtStep());

            paymentService.transitionPaymentStatus(
                    event.paymentId(),
                    PaymentStatus.failed(event.failureReason(),step.name()),
                    event.correlationId(),
                    "Saga failed at step: "+ event.failedAtStep()
            );
            if(event.debitApplied()){
                ledgerService.reverseLedgerEntries(event.paymentId(),event.correlationId());
            }

            meterRegistry.counter("saga.event.processed","event", Constants.PaymentStatus.FAILED).increment();
            meterRegistry.counter("payment.failed","step",event.failedAtStep()).increment();
            ack.acknowledge();
            LOG.warn("SAGA_FAILED : paymentId={},reason={}",event.paymentId(),event.failureReason());
        });
    }

    /**  payment.compensate.debit --> reverse debited payment  */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_COMPENSATE_DEBIT,
            groupId = KafkaConstants.KAFKA_COMPENSATE_GROUP,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void onCompensationDebit(ConsumerRecord<String,SagaEvent> record, Acknowledgment ack){
        var event = (CompensateDebitEvent)record.value();
        var sagaKey = "compensate:" + event.paymentId();

        moveMDC(event.paymentId(), event.correlationId(), "COMPENSATE",()->{
            if(idempotencyHandlers.alreadyProcessed(sagaKey)){
                LOG.warn("SAGA_COMPENSATE_SKIP : already compensated");
                ack.acknowledge();
                return;
            }
            try{
                accountService.processCompensatingCredit(event);
                idempotencyHandlers.markProcessed(sagaKey);
                meterRegistry.counter("saga.compensation.applied").increment();
                ack.acknowledge();
            }catch (Exception ex){
                LOG.error("SAGA_COMPENSATE_ERROR : paymentId={} - Manual Check Needed",event.paymentId(),ex);
                meterRegistry.counter("saga.compensation.failed").increment();
                throw ex;
            }
        });

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
