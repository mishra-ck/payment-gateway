package payment.gateway.controller;

import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import payment.gateway.domain.dto.PaymentHistoryResponse;
import payment.gateway.domain.dto.PaymentRequest;
import payment.gateway.domain.dto.PaymentResponse;
import payment.gateway.service.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping(Constants.Endpoint.BASE_PATH )
@Slf4j
@Validated
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Realtime Payment Processing API ")
public class PaymentProcessController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentProcessController.class);
    private final PaymentService paymentService = null;

    /**--- Initiate Payment End Point  ----*/
    @PostMapping(Constants.Endpoint.VERSION_V1 + Constants.Endpoint.PAYMENTS)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Initiate a payment",
            description = "Create a new payment and starts the saga." +
                    "Idempotent - returns same result with same X-Idempotency-Key",
            parameters = {
                    @Parameter(name = "X-Idempotency-Key", required = true,
                    description = "Client generated UUID, stable across retries.")
            },
            responses = {
                    @ApiResponse(responseCode = "201",description = "Payment initiated"),
                    @ApiResponse(responseCode = "400",description = "Validation error"),
                    @ApiResponse(responseCode = "409",description = "Duplicate idempotency key with different body"),
                    @ApiResponse(responseCode = "429",description = "Rate limit exceeded"),
                    @ApiResponse(responseCode = "503",description = "Service unavailable"),
            }
    )
    @Observed(name = "api.payment.initiated")
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

    /** ----------  GET Payment Details ---------- */
    @GetMapping(Constants.Endpoint.VERSION_V1 + "/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentDetails(
            @PathVariable UUID paymentId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        MDC.put("paymentId", paymentId.toString());
        try {
            LOG.debug("API Get Payment, paymentId = {}", paymentId);
            var response = paymentService.getPaymentDetails(paymentId);

            var headers = new HttpHeaders();
            headers.set("X-Payment-Id", paymentId.toString());
            if (requestId != null) {
                headers.set("X-Request-Id", requestId);
            }
            return ResponseEntity.ok().header(String.valueOf(headers)).body(response);

        } finally {
            MDC.remove("paymentId");
        }
    }

    /** ------- GET Payment history ----------- */
    @GetMapping(Constants.Endpoint.VERSION_V1+"/{paymentId}/history")
    public ResponseEntity<PaymentHistoryResponse> getPaymentHistory(
            @PathVariable UUID paymentId
    ){
        LOG.debug("API Get History: paymentId={}", paymentId);
        return ResponseEntity.ok(paymentService.getPaymentHistory(paymentId));
    }


}
