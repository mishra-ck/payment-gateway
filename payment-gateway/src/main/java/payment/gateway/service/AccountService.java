package payment.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.gateway.repository.AccountRepository;
import payment.gateway.repository.TransactionRepository;
import payment.gateway.saga.events.PaymentInitiatedEvent;
import payment.gateway.saga.events.SagaEvent;

@Service
@Slf4j
public class AccountService {
    private final static Logger LOG = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    private final KafkaTemplate<String, SagaEvent> kafkaTemplate ;
    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository, KafkaTemplate<String, SagaEvent> kafkaTemplate) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
    @Transactional
    public void processDebit(PaymentInitiatedEvent event) {
        LOG.info("SAGA_DEBIT_START : paymentId :{},account={}, amount={}, currency={}",
                event.paymentId(),event.sourceAccountId(),event.amount(),event.currency());

        try{
            /*TBD*/
        }catch (Exception ex){

        }

    }
}
