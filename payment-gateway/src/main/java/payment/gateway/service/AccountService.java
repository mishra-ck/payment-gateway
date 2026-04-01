package payment.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.gateway.config.enums.TransactionType;
import payment.gateway.domain.model.Account;
import payment.gateway.domain.model.Transaction;
import payment.gateway.exception.InvalidPaymentException;
import payment.gateway.infrastructure.kafka.KafkaConfig;
import payment.gateway.infrastructure.redis.DistributedLockService;
import payment.gateway.repository.AccountRepository;
import payment.gateway.repository.TransactionRepository;
import payment.gateway.saga.events.*;

import java.time.Instant;

@Service
@Slf4j
public class AccountService {
    private final static Logger LOG = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final DistributedLockService  lockService;

    private final KafkaTemplate<String, SagaEvent> kafkaTemplate ;
    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                          DistributedLockService lockService, KafkaTemplate<String, SagaEvent> kafkaTemplate) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.lockService = lockService;
        this.kafkaTemplate = kafkaTemplate;
    }
    // Saga : Handle Payment Initiated Event --> Apply Debit Event
    @Transactional
    public void processDebit(PaymentInitiatedEvent event) {
        LOG.info("SAGA_DEBIT_START : paymentId :{},account={}, amount={}, currency={}",
                event.paymentId(),event.sourceAccountId(),event.amount(),event.currency());

        lockService.withAccountLock(event.sourceAccountId(), () ->{
            var account = accountRepository.findByIdWithLock(event.sourceAccountId());
            if(account.isEmpty()){
                throw new InvalidPaymentException("Source account not found : "+ event.sourceAccountId());
            }
            try{
                Account debitAccount = account.get();
                debitAccount.debit(event.amount());
                accountRepository.save(debitAccount);

                var txn = Transaction.debit(
                        event.paymentId(),
                        event.sourceAccountId(),
                        event.amount(),
                        event.currency(),
                        debitAccount.getAvailableBalance(),
                        event.correlationId()
                );
                transactionRepository.save(txn);

                /* Publish Debit Event - Trigger Credit Event */
                var debitEvent = new PaymentDebitedEvent(
                        event.paymentId(),
                        event.correlationId(),
                        event.sourceAccountId(),
                        event.destinationAccountId(),
                        event.amount(),
                        event.currency(),
                        debitAccount.getAvailableBalance(),
                        txn.getId(),
                        Instant.now()
                );

                kafkaTemplate.send(
                        KafkaConfig.TOPIC_PAYMENT_DEBITED,
                        event.paymentId().toString(),debitEvent );

                LOG.info("SAGA_DEBIT_OK : paymentId :{},txnId:{},balanceAfter:{}",
                        event.paymentId(),txn.getId(),debitAccount.getAvailableBalance());

            }catch (Exception ex){
                LOG.warn("SAGA_DEBIT_INSUFFICIENT_FUNDS : paymentId :{}, reason:{}",
                        event.paymentId(),ex.getMessage());
                kafkaTemplate.send(
                        KafkaConfig.TOPIC_PAYMENT_FAILED,
                        event.paymentId().toString(),
                        new PaymentFailedEvent(
                                event.paymentId(),
                                event.correlationId(),
                                "In-sufficient Funds: "+ex.getMessage(),
                                TransactionType.DEBIT.name(),
                                false,
                                Instant.now()
                        )
                );
            }
            return null;
        });
    }

    // Saga : Handle Payment Debit Event --> Trigger Credit Event
    public void processCredit(PaymentDebitedEvent event){
        LOG.info("SAGA_CREDIT_START : paymentId :{},account={}, amount={}, currency={}",
                event.paymentId(),event.destinationAccountId(),event.amount(),event.currency());

        lockService.withAccountLock(event.destinationAccountId(), () ->{

            var account = accountRepository.findByIdWithLock(event.destinationAccountId())
                    .orElseThrow( () -> {
                        publishCompensation(event,"Destination Account Not Found");
                        return new InvalidPaymentException("Destination Account Not Found");
                    });
            try{
                account.credit(event.amount());
                accountRepository.save(account);

                var txn = Transaction.credit(
                        event.paymentId(),
                        event.destinationAccountId(),
                        event.amount(),
                        event.currency(),
                        account.getAvailableBalance(),
                        event.correlationId()
                );
                transactionRepository.save(txn);

                var creditEvent = new PaymentCreditedEvent(
                        event.paymentId(),
                        event.correlationId(),
                        event.sourceAccountId(),
                        event.destinationAccountId(),
                        event.amount(),
                        event.currency(),
                        account.getAvailableBalance(),
                        txn.getId(),
                        Instant.now()
                );

                kafkaTemplate.send(
                        KafkaConfig.TOPIC_PAYMENT_CREDITED,
                        event.paymentId().toString(),
                        creditEvent
                );
                LOG.info("SAGA_CREDIT_OK:paymentId:{}, txnId:{}",event.paymentId()
                ,txn.getId());

            }catch(Exception ex){
                LOG.error("SAGA_CREDIT_FAILED: paymentId:{}, error:{}",event.paymentId(),ex.getMessage());
                publishCompensation(event, ex.getMessage());
            }
            return null;
        });

    }
    // Compensate the debit back to the source account if credit fails.
    private void publishCompensation(PaymentDebitedEvent event, String reason) {
        kafkaTemplate.send(KafkaConfig.TOPIC_COMPENSATE_DEBIT,
                event.paymentId().toString(),
                new CompensateDebitEvent(
                        event.paymentId(),
                        event.correlationId(),
                        event.sourceAccountId(),
                        event.amount(),
                        event.currency(),
                        reason,
                        Instant.now()
                )
        );
        kafkaTemplate.send(
                KafkaConfig.TOPIC_PAYMENT_FAILED,
                event.paymentId().toString(),
                new PaymentFailedEvent(
                        event.paymentId(),
                        event.correlationId(),
                        reason,
                        TransactionType.CREDIT.name(),
                        true,
                        Instant.now()
                )
        );
    }


    public void processCompensatingCredit(CompensateDebitEvent event) {
        /*TODO*/
    }

    public void processCredit(PaymentInitiatedEvent event) {
        /*TODO*/
    }
}
