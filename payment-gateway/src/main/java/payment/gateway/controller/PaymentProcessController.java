package payment.gateway.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import payment.gateway.config.constants.Constants;
import payment.gateway.domain.dto.PaymentRequest;
import payment.gateway.domain.dto.PaymentResponse;

@RestController
@RequestMapping(Constants.BASE_PATH )
@Slf4j
@Validated
public class PaymentProcessController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentProcessController.class);
    @PostMapping(Constants.VERSION_V1 + Constants.PAYMENTS)
    public ResponseEntity<PaymentResponse> initiatePayment(
        @RequestHeader("X-Idempotency-Key")
        @NotBlank(message = "X-Idempotency-Key header is required")
        @Size(min = 8,max = 64)
        String idempotencyKey,
        @RequestHeader(value = "X-Request-Id",required = false)
        String requestId ,
        @Valid
        @RequestBody PaymentRequest request){

        MDC.put("idempotencyKey", idempotencyKey);
        MDC.put("requestId", requestId != null ? requestId : "none");
        try{
            LOG.info("Payment Initiate..");  /* TBD */
        }finally {
            MDC.remove("idempotencyKey");
            MDC.remove("requestId");
        }

    }

}
