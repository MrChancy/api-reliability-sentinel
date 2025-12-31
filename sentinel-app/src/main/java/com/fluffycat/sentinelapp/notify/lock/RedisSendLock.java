package com.fluffycat.sentinelapp.notify.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSendLock {
    private final StringRedisTemplate redis;

    /**
     * 去重节流锁
     * @param dedupeKey 去重key
     * @param ttl 锁持续时间
     * @return true = 拿到锁(允许发送); false = 未拿到锁(抑制发送)
     * 降级策略：Redis异常时返回true(Fail-Open, 不影响主流程)
     */
    public boolean tryAcquire(String dedupeKey, Duration ttl){
        String key = "send-lock:"+dedupeKey;
        String value = UUID.randomUUID().toString();

        try {
            Boolean ok = redis.opsForValue().setIfAbsent(key, value, ttl);
            return Boolean.TRUE.equals(ok);
        }catch (RuntimeException ex){
            log.warn("Redis unavailable, bypass send-lock, key={}",key,ex);
            return true;
        }
    }
}
