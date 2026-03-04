package com.qfeed.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;


@Component
public class JwtService {


    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-ttl-seconds}") long accessTtlSeconds,
            @Value("${jwt.refresh-ttl-seconds}") long refreshTtlSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public String newJti() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public String issueRefreshToken(Long userId, String username, String jti) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshTtlSeconds);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("typ", "refresh")
                .claim("jti", jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public String issueAccessToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtlSeconds);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public RefreshClaims parseAndValidateRefresh(String token) {
        Claims c = parseAndValidate(token);

        String typ = c.get("typ", String.class);
        if (!"refresh".equals(typ)) {
            throw new IllegalArgumentException("token type mismatch");
        }

        String jti = c.get("jti", String.class);
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("missing jti");
        }

        Long userId = Long.valueOf(c.getSubject());
        String username = c.get("username", String.class);
        Instant exp = c.getExpiration().toInstant();

        return new RefreshClaims(userId, username, jti, exp);
    }


    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    public static class RefreshClaims {
        public final Long userId;
        public final String username;
        public final String jti;
        public final Instant expiresAt;

        public RefreshClaims(Long userId, String username, String jti, Instant expiresAt) {
            this.userId = userId;
            this.username = username;
            this.jti = jti;
            this.expiresAt = expiresAt;
        }
    }
}

