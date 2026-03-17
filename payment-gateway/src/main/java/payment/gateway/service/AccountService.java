package payment.gateway.service;

import org.springframework.stereotype.Service;
import payment.gateway.saga.events.PaymentInitiatedEvent;

@Service
public class AccountService {
    public void processDebit(PaymentInitiatedEvent event) {
        /*TBD*/
    }
}
