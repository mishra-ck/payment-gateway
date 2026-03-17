package payment.gateway.saga.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IdempotencyHandlers {
    public boolean alredyProcessed(String sagaKey) {
        /*TBD*/
        return false;
    }

    public void markProcessed(String sagaKey) {
        /*TBD*/
    }
}
