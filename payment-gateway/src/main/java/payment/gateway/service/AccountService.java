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
import payment.gateway.saga.events.PaymentDebitedEvent;
import payment.gateway.saga.events.PaymentFailedEvent;
import payment.gateway.saga.events.PaymentInitiatedEvent;
import payment.gateway.saga.events.SagaEvent;

import java.time.Instant;

@Service
@Slf4j
public class AccountService {
    private final static Logger LOG = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final DistributedLockService  lockService;

    private final KafkaTemplate<String, SagaEvent> kafkaTemplate ;
    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository, DistributedLockService lockService, KafkaTemplate<String, SagaEvent> kafkaTemplate) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.lockService = lockService;
        this.kafkaTemplate = kafkaTemplate;
    }
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

}
