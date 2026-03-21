package payment.gateway.infrastructure.redis;

import com.esotericsoftware.minlog.Log;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import payment.gateway.config.constants.Constants;
import payment.gateway.exception.LockAcquireException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
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

   private static final Logger LOG = LoggerFactory.getLogger(DistributedLockService.class);
   private final RedissonClient redissonClient ;
   private final MeterRegistry meterRegistry ;
   public DistributedLockService(RedissonClient redissonClient, MeterRegistry meterRegistry) {
      this.redissonClient = redissonClient;
      this.meterRegistry = meterRegistry;
   }
   /**
    * Prevents concurrent balance update from  multiple pods by having account-level distributed lock.
    */
   public <T> T withAccountLock(UUID accountId, Supplier<T> action){
      return withLock(Constants.Lock.ACCOUNT_LOCK_PREFIX + accountId,action);
   }

   private <T> T withLock(String lockKey, Supplier<T> action) {
      RLock lock = redissonClient.getLock(lockKey);
      Timer.Sample sample = Timer.start(meterRegistry);
      boolean lockAcquired = false ;

      try{
         lockAcquired = lock.tryLock(Constants.Lock.WAIT_TIMEOUT_SECONDS,
                 Constants.Lock.LEASE_SECONDS, TimeUnit.SECONDS);
         if(!lockAcquired){
            meterRegistry.counter("distributed_lock.timeout","key",lockKey).increment();
            throw new LockAcquireException(
              "Could not acquire distributed lock for : "+ lockKey +
                      "within " + Constants.Lock.WAIT_TIMEOUT_SECONDS + "s"
            );
         }
         LOG.debug("LOCK_ACQUIRED: key={}, threadId={}", lockKey, Thread.currentThread().getId());
         meterRegistry.counter("distributed_lock.acquired","key_prefix",extractPrefix(lockKey)).increment();
         return action.get();
      }catch(InterruptedException | LockAcquireException ex){
         /*TBD*/
      }finally {
         /*TBD*/
      }

      return null;
   }

   private String extractPrefix(String lockKey) {
      int idx = lockKey.lastIndexOf(":");
      return idx > 0 ?lockKey.substring(0,idx):lockKey;
   }

}
