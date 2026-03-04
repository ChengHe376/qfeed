package com.qfeed.auth.dto;

public class AuthResponse {
    public Long userId;
    public String username;
    public String accessToken;
    public String refreshToken;

    public AuthResponse(Long userId, String username, String accessToken) {
        this(userId, username, accessToken, null);
    }

    public AuthResponse(Long userId, String username, String accessToken, String refreshToken) {
        this.userId = userId;
        this.username = username;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}