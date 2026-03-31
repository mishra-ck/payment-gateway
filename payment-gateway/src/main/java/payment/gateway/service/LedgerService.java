package payment.gateway.service;

import org.springframework.stereotype.Service;
import payment.gateway.saga.events.PaymentCreditedEvent;

import java.util.UUID;

@Service
public class LedgerService {
    public void recordPaymentLedgerEntries(PaymentCreditedEvent event) {
        /*TODO*/
    }
    public void reverseLedgerEntries(UUID uuid, String s) {
        /*TODO*/
    }
}
