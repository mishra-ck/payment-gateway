package payment.gateway.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import payment.gateway.config.constants.Constants;
import payment.gateway.domain.dto.PaymentRequest;
import payment.gateway.domain.dto.PaymentResponse;
import payment.gateway.service.PaymentService;

@RestController
@RequestMapping(Constants.Endpoint.BASE_PATH )
@Slf4j
@Validated
@RequiredArgsConstructor
public class PaymentProcessController {
    private final PaymentService paymentService = null;
    private static final Logger LOG = LoggerFactory.getLogger(PaymentProcessController.class);

    @PostMapping(Constants.Endpoint.VERSION_V1 + Constants.Endpoint.PAYMENTS)
    @ResponseStatus(HttpStatus.CREATED)
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
            LOG.info("Payment Initiate..");
            var response = paymentService.initiatePayment(request,idempotencyKey);

            var headers = new HttpHeaders();
            headers.set("X-Payment-Id",response.id().toString());
            headers.set("X-Idempotency-Key",idempotencyKey);
            if(requestId != null){
                headers.set("X-Request-Id",requestId);
            }
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .headers(headers)
                    .body(response);
        }finally {
            MDC.remove("idempotencyKey");
            MDC.remove("requestId");
        }
    }

}
