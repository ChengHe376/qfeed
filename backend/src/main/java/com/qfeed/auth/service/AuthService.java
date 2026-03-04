package com.qfeed.auth.service;

import com.qfeed.auth.dto.AuthResponse;
import com.qfeed.auth.dto.LoginRequest;
import com.qfeed.auth.dto.RegisterRequest;
import com.qfeed.user.domain.User;
import com.qfeed.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.qfeed.auth.jwt.JwtService;
import com.qfeed.auth.security.LoginGuardService;
import com.qfeed.auth.mapper.RefreshTokenMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import com.qfeed.auth.security.RefreshTokenRecord;
import org.springframework.data.redis.core.StringRedisTemplate;




@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginGuardService loginGuardService;
    private final RefreshTokenMapper refreshTokenMapper;
    private final StringRedisTemplate redis;


    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService, LoginGuardService loginGuardService, RefreshTokenMapper refreshTokenMapper, StringRedisTemplate redis) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginGuardService = loginGuardService;
        this.refreshTokenMapper = refreshTokenMapper;
        this.redis = redis;
    }


    @Transactional
    public AuthResponse register(RegisterRequest req) {
        User exist = userMapper.findByUsername(req.username);
        if (exist != null) {
            throw new IllegalArgumentException("username already exists");
        }

        User u = new User();
        u.username = req.username;
        u.passwordHash = passwordEncoder.encode(req.password);

        userMapper.insertUser(u);
        String token = jwtService.issueAccessToken(u.id, u.username);
        return new AuthResponse(u.id, u.username, token);

    }

    @Transactional
    public AuthResponse login(LoginRequest req, String ip) {
        String username = (req.username == null ? "" : req.username.trim());
        String password = (req.password == null ? "" : req.password);

        if (username.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("username/password required");
        }

        loginGuardService.checkBeforeLogin(username, ip);

        User u = userMapper.findByUsername(username);
        if (u == null) {
            loginGuardService.onLoginFailure(username);
            throw new IllegalArgumentException("invalid credentials");
        }

        if (!passwordEncoder.matches(password, u.passwordHash)) {
            loginGuardService.onLoginFailure(username);
            throw new IllegalArgumentException("invalid credentials");
        }

        loginGuardService.onLoginSuccess(username);

        String accessToken = jwtService.issueAccessToken(u.id, u.username);

        String refreshJti = jwtService.newJti();
        String refreshToken = jwtService.issueRefreshToken(u.id, u.username, refreshJti);

        String tokenHash = sha256(refreshToken);

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtService.getRefreshTtlSeconds());

        refreshTokenMapper.insert(u.id, refreshJti, tokenHash, expiresAt);

        return new AuthResponse(u.id, u.username, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken, String ip) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken required");
        }

        JwtService.RefreshClaims c = jwtService.parseAndValidateRefresh(refreshToken);
        String jti = c.jti;

        // 1) Redis 快速判撤销
        String revokedKey = "auth:rt:revoked:" + jti;
        if (redis.opsForValue().get(revokedKey) != null) {
            throw new IllegalArgumentException("refresh token revoked");
        }

        // 2) DB 权威校验
        RefreshTokenRecord r = refreshTokenMapper.findByJti(jti);
        if (r == null) {
            throw new IllegalArgumentException("refresh token invalid");
        }
        if (r.revokedAt != null) {
            throw new IllegalArgumentException("refresh token revoked");
        }
        if (r.expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("refresh token expired");
        }

        String hash = sha256(refreshToken);
        if (!hash.equals(r.tokenHash)) {
            throw new IllegalArgumentException("refresh token invalid");
        }

        // 3) 轮换：撤销旧的 + 发新的
        String newJti = jwtService.newJti();
        String newRefreshToken = jwtService.issueRefreshToken(c.userId, c.username, newJti);

        String newHash = sha256(newRefreshToken);
        LocalDateTime newExp = LocalDateTime.now().plusSeconds(jwtService.getRefreshTtlSeconds());

        int updated = refreshTokenMapper.revokeByJti(jti, LocalDateTime.now(), newJti);
        if (updated != 1) {
            throw new IllegalArgumentException("refresh token already used or revoked");
        }

        refreshTokenMapper.insert(c.userId, newJti, newHash, newExp);

        // 4) Redis 写撤销缓存：旧 token 立即失效
        long ttlSeconds = Math.max(1, java.time.Duration.between(LocalDateTime.now(), r.expiresAt).getSeconds());
        redis.opsForValue().set(revokedKey, "1", java.time.Duration.ofSeconds(ttlSeconds));

        // 5) 新 access
        String accessToken = jwtService.issueAccessToken(c.userId, c.username);

        return new AuthResponse(c.userId, c.username, accessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        JwtService.RefreshClaims c;
        try {
            c = jwtService.parseAndValidateRefresh(refreshToken);
        } catch (Exception e) {
            return;
        }

        String jti = c.jti;

        RefreshTokenRecord r = refreshTokenMapper.findByJti(jti);
        if (r == null) {
            return;
        }

        if (r.revokedAt == null) {
            refreshTokenMapper.revokeByJtiForce(jti, LocalDateTime.now());
        }

        long ttlSeconds = Math.max(1, java.time.Duration.between(LocalDateTime.now(), r.expiresAt).getSeconds());
        redis.opsForValue().set("auth:rt:revoked:" + jti, "1", java.time.Duration.ofSeconds(ttlSeconds));
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



}
