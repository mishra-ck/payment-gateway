package payment.gateway.saga.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Idempotency guard for SAGA Event Handlers
 * Prevents Kafka Message re-delivery from causing duplication
 * Key expires after 24 Hrs -- good for retry window
 * Key-format : "saga.processed:{sagaKey}"
 */
@Component
@Slf4j
public class IdempotencyHandlers {
    private static final String PREFIX = "saga:processed:";
    private static final Duration TTL = Duration.ofHours(24); // Time-to-live is 24 Hrs
    private final StringRedisTemplate redisTemplate;
    public IdempotencyHandlers(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    /** @return true is the saga step is already processed successfully */
    public boolean alreadyProcessed(String sagaKey) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(PREFIX+sagaKey)
        );
    }
    public void markProcessed(String sagaKey) {
        /*TBD*/
    }
}
