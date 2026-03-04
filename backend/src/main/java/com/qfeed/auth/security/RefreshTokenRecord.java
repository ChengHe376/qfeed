package com.qfeed.auth.security;

public class RefreshTokenRecord {
    public Long id;
    public Long userId;
    public String jti;
    public String tokenHash;
    public java.time.LocalDateTime expiresAt;
    public java.time.LocalDateTime revokedAt;
    public String replacedByJti;
    public java.time.LocalDateTime createdAt;
}
