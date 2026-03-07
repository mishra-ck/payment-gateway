package payment.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import payment.gateway.domain.dto.PaymentRequest;
import payment.gateway.domain.dto.PaymentResponse;
import payment.gateway.repository.PaymentRepository;

@Service
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository ;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse initiatePayment(PaymentRequest request, String idempotencyKey) {
        /* TBD */
        return null;
    }
}
