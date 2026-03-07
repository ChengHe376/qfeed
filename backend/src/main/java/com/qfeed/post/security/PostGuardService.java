package com.qfeed.post.security;

import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

import com.qfeed.auth.security.TooManyRequestsException;

@Service
public class PostGuardService {

    private final StringRedisTemplate redis;

    private final int maxPosts;
    private final long windowSeconds;

    public PostGuardService(
            StringRedisTemplate redis,
            @Value("${post.create.max-per-window:3}") int maxPosts,
            @Value("${post.create.window-seconds:10}") long windowSeconds
    ) {
        this.redis = redis;
        this.maxPosts = maxPosts;
        this.windowSeconds = windowSeconds;
    }

    private String rateKey(Long userId) {
        return "post:create:rate:u:" + userId;
    }

    public void checkCreatePost(Long userId) {

        String key = rateKey(userId);

        Long count = redis.opsForValue().increment(key);

        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofSeconds(windowSeconds));
        }

        if (count != null && count > maxPosts) {
            throw new TooManyRequestsException("posting too frequently");
        }
    }
}