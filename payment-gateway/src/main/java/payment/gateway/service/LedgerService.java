package payment.gateway.service;

import org.springframework.stereotype.Service;
import payment.gateway.saga.events.PaymentCreditedEvent;

@Service
public class LedgerService {
    public void recordPaymentLedgerEntries(PaymentCreditedEvent event) {
        /*TODO*/
    }
}
