package com.qfeed.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LoginGuardService {

    private final StringRedisTemplate redis;

    private final int maxFailures;
    private final long lockSeconds;
    private final long ipWindowSeconds;
    private final int ipMaxRequests;
    private final long failWindowSeconds;

    public LoginGuardService(
            StringRedisTemplate redis,
            @Value("${auth.login.max-failures}") int maxFailures,
            @Value("${auth.login.lock-seconds}") long lockSeconds,
            @Value("${auth.login.fail-window-seconds:${auth.login.lock-seconds}}") long failWindowSeconds,
            @Value("${auth.login.ip-window-seconds}") long ipWindowSeconds,
            @Value("${auth.login.ip-max-requests}") int ipMaxRequests
    ) {
        this.redis = redis;
        this.maxFailures = maxFailures;
        this.lockSeconds = lockSeconds;
        this.failWindowSeconds = failWindowSeconds;
        this.ipWindowSeconds = ipWindowSeconds;
        this.ipMaxRequests = ipMaxRequests;
    }

    private String userFailKey(String username) {
        return "auth:login:fail:u:" + username;
    }

    private String userLockKey(String username) {
        return "auth:login:lock:u:" + username;
    }

    private String ipRateKey(String ip) {
        return "auth:login:rate:ip:" + ip;
    }

    public void checkBeforeLogin(String username, String ip) {
        // 1) 先做 IP rate limit
        String ipKey = ipRateKey(ip);
        Long ipCount = redis.opsForValue().increment(ipKey);
        if (ipCount != null && ipCount == 1L) {
            redis.expire(ipKey, Duration.ofSeconds(ipWindowSeconds));
        }
        if (ipCount != null && ipCount > ipMaxRequests) {
            throw new TooManyRequestsException("Too many login attempts from this IP");
        }

        // 2) 再检查账号锁定
        String lockKey = userLockKey(username);
        String locked = redis.opsForValue().get(lockKey);
        if (locked != null) {
            throw new UserLockedException("Account locked, try later");
        }
    }


    public void onLoginSuccess(String username) {
        redis.delete(userFailKey(username));
    }


    public void onLoginFailure(String username) {
        String failKey = userFailKey(username);
        Long v = redis.opsForValue().increment(failKey);

        if (v != null && v == 1L) {
            redis.expire(failKey, Duration.ofSeconds(failWindowSeconds));
        }

        if (v != null && v >= maxFailures) {
            String lockKey = userLockKey(username);
            redis.opsForValue().set(lockKey, "1", Duration.ofSeconds(lockSeconds));
        }
    }

    private boolean isUserLocked(String username) {
        String v = redis.opsForValue().get(userLockKey(username));
        return v != null;
    }

    private boolean isIpRateLimited(String ip) {
        String key = ipRateKey(ip);
        Long v = redis.opsForValue().increment(key);

        if (v != null && v == 1L) {
            redis.expire(key, Duration.ofSeconds(ipWindowSeconds));
        }
        return v != null && v > ipMaxRequests;
    }
}
