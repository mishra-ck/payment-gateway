package payment.gateway.saga.handlers;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import payment.gateway.service.AccountService;
import payment.gateway.service.LedgerService;
import payment.gateway.service.PaymentService;

/**
 * SAGA Choreography event handlers
 */
@Component
@Slf4j
public class EventHandlers {
    private final AccountService accountService;
    private final LedgerService ledgerService;
    private final PaymentService paymentService;
    private final IdempotencyHandlers idempotencyHandlers;
    private final MeterRegistry meterRegistry;

    public EventHandlers(AccountService accountService, LedgerService ledgerService,
                         PaymentService paymentService, IdempotencyHandlers idempotencyHandlers,
                         MeterRegistry meterRegistry) {
        this.accountService = accountService;
        this.ledgerService = ledgerService;
        this.paymentService = paymentService;
        this.idempotencyHandlers = idempotencyHandlers;
        this.meterRegistry = meterRegistry;
    }
}
