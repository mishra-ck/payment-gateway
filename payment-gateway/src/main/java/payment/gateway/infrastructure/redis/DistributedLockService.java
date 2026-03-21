package payment.gateway.infrastructure.redis;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import payment.gateway.config.constants.Constants;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Distributed Lock service - prevent concurrent balance mutations on same account
 * across multiple pods.
 * Lock Key format : "account:lock:{accountId}"
 * Lock TTL : 30 sec (auto-release on pod crash - watch dog mechanism)
 * Redisson's fair lock ensures:
 * - Only one thread across cluster holds the lock at a time.
 * - Lock is auto-renewed while the holder is alive
 * - Released on un-checked exception (no lock leak)
 */
@Service
@Slf4j
public class DistributedLockService {
   private final RedissonClient redissonClient ;
   private final MeterRegistry meterRegistry ;
   public DistributedLockService(RedissonClient redissonClient, MeterRegistry meterRegistry) {
      this.redissonClient = redissonClient;
      this.meterRegistry = meterRegistry;
   }

   public <T> T withAccountLock(UUID accountId, Supplier<T> action){
      return withLock(Constants.Lock.ACCOUNT_LOCK_PREFIX + accountId,action);
   }

   private <T> T withLock(String s, Supplier<T> action) {
      /*TBD*/
      return null;
   }

}
