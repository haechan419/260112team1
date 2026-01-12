package com.Team1_Back.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisLock {

    private final StringRedisTemplate redis;

    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redis.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    public void unlock(String key, String token) {
        String cur = redis.opsForValue().get(key);
        if (token != null && token.equals(cur)) redis.delete(key);
    }
}
