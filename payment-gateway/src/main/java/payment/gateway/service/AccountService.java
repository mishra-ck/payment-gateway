package payment.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.gateway.domain.model.Account;
import payment.gateway.exception.InvalidPaymentException;
import payment.gateway.infrastructure.redis.DistributedLockService;
import payment.gateway.repository.AccountRepository;
import payment.gateway.repository.TransactionRepository;
import payment.gateway.saga.events.PaymentInitiatedEvent;
import payment.gateway.saga.events.SagaEvent;

import java.util.Optional;

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
            }catch (Exception ex){

            }

            return null;
        });
        try{

        }catch (Exception ex){

        }

    }
}
