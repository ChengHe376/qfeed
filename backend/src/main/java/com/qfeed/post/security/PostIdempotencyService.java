package com.qfeed.post.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class PostIdempotencyService {

    private final StringRedisTemplate redis;
    private final long resultTtlSeconds;
    private final long lockTtlSeconds;

    public PostIdempotencyService(
            StringRedisTemplate redis,
            @Value("${post.create.idempotency-result-ttl-seconds:86400}") long resultTtlSeconds,
            @Value("${post.create.idempotency-lock-ttl-seconds:10}") long lockTtlSeconds
    ) {
        this.redis = redis;
        this.resultTtlSeconds = resultTtlSeconds;
        this.lockTtlSeconds = lockTtlSeconds;
    }

    private String resultKey(Long userId, String clientRequestId) {
        return "post:create:idem:u:" + userId + ":req:" + clientRequestId + ":result";
    }

    private String lockKey(Long userId, String clientRequestId) {
        return "post:create:idem:u:" + userId + ":req:" + clientRequestId + ":lock";
    }

    public Long getExistingPostId(Long userId, String clientRequestId) {
        String v = redis.opsForValue().get(resultKey(userId, clientRequestId));
        if (v == null || v.isBlank()) {
            return null;
        }
        return Long.valueOf(v);
    }

    public boolean tryAcquireLock(Long userId, String clientRequestId) {
        Boolean ok = redis.opsForValue().setIfAbsent(
                lockKey(userId, clientRequestId),
                "1",
                Duration.ofSeconds(lockTtlSeconds)
        );
        return Boolean.TRUE.equals(ok);
    }

    public void recordSuccess(Long userId, String clientRequestId, Long postId) {
        redis.opsForValue().set(
                resultKey(userId, clientRequestId),
                String.valueOf(postId),
                Duration.ofSeconds(resultTtlSeconds)
        );
    }

    public void releaseLock(Long userId, String clientRequestId) {
        redis.delete(lockKey(userId, clientRequestId));
    }
}