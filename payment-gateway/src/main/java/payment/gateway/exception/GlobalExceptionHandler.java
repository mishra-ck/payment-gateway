package payment.gateway.exception;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import payment.gateway.domain.dto.ErrorResponse;

import java.time.Instant;
import java.util.List;

/**
 * Global Exception Handler
 * Error metrics are recorded for alerting
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex,
                                                        HttpServletRequest request){
        return error(HttpStatus.NOT_FOUND,"PAYMENT_NOT_FOUND", ex.getMessage(),List.of());
    }
    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPayment(InvalidPaymentException ex,
                                                              HttpServletRequest request){
        counter("INVALID_PAYMENT");
        return error(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT",ex.getMessage(),List.of());
    }
    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> handleDuplication(DuplicatePaymentException ex,
                                                           HttpServletRequest request){
        counter("DUPLICATE_PAYMENT");
        return error(HttpStatus.CONFLICT,"DUPLICATE_PAYMENT",ex.getMessage(),List.of());
    }
    @ExceptionHandler(InsufficientFundException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFund(InsufficientFundException ex,
                                                                HttpServletRequest request){
        counter("INSUFFICIENT_FUNDS");
        return error(HttpStatus.UNPROCESSABLE_ENTITY,"INSUFFICIENT_FUNDS",ex.getMessage(),List.of());
    }
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex,
                                                         HttpServletRequest request){
        counter("RATE_LIMIT_EXCEEDED");
        return error(HttpStatus.TOO_MANY_REQUESTS,"RATE_LIMIT_EXCEEDED",ex.getMessage(),List.of());
    }


    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code,
                                                String message, List<ErrorResponse.FieldError> fieldErrors){
        var traceId = tracer.currentSpan() != null
                ? tracer.currentSpan().context().traceId()
                : "no-trace";
        return ResponseEntity.status(status).body(
          new ErrorResponse(code,message,traceId, Instant.now(),fieldErrors)
        );
    }
    private void counter(String errorType) {
        meterRegistry.counter("api.errors", "type", errorType).increment();
    }
}
