package payment.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.gateway.saga.events.PaymentInitiatedEvent;

@Service
@Slf4j
public class AccountService {
    private final static Logger LOG = LoggerFactory.getLogger(AccountService.class);
    @Transactional
    public void processDebit(PaymentInitiatedEvent event) {
        LOG.info("SAGA_DEBIT_START : paymentId :{},account={}, amount={}, currency={}",
          event.paymentId(),event.sourceAccountId(),event.amount(),event.currency());
    }
}
