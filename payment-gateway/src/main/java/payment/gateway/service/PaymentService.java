package payment.gateway.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import payment.gateway.domain.dto.PaymentRequest;
import payment.gateway.domain.dto.PaymentResponse;
import payment.gateway.repository.IdempotencyRepository;
import payment.gateway.repository.PaymentRepository;

@Service
@Slf4j
public class PaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository ;
    private final IdempotencyRepository idempotencyRepository;

    public PaymentService(PaymentRepository paymentRepository, IdempotencyRepository idempotencyRepository) {
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
    }

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request, String idempotencyKey) {
        /* TBD - rate limiter need to be added */
        return doInitiatePayment(request,idempotencyKey);
    }
    private PaymentResponse doInitiatePayment(PaymentRequest request, String idempotencyKey){
        MDC.put("idempotencyKey",idempotencyKey);

        try{
            /** ------ Idempotency Check ---------- **/
            var existing = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
            if(existing.isPresent()){
                LOG.info("Idempotency Hit : key{}",idempotencyKey);

            }

        }finally {

        }

        return null;
    }
}
