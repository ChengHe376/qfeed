package com.qfeed.auth.controller;

import com.qfeed.auth.dto.AuthResponse;
import com.qfeed.auth.dto.LoginRequest;
import com.qfeed.auth.dto.RegisterRequest;
import com.qfeed.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import com.qfeed.auth.dto.RefreshRequest;
import org.springframework.data.redis.core.StringRedisTemplate;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        String xri = request.getHeader("X-Real-IP");

        String ip;
        if (xff != null && !xff.isBlank()) {
            ip = xff.split(",")[0].trim();
        } else if (xri != null && !xri.isBlank()) {
            ip = xri.trim();
        } else {
            ip = request.getRemoteAddr();
        }

        return ResponseEntity.ok(authService.login(req, ip));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest req, HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        String xri = request.getHeader("X-Real-IP");

        String ip;
        if (xff != null && !xff.isBlank()) {
            ip = xff.split(",")[0].trim();
        } else if (xri != null && !xri.isBlank()) {
            ip = xri.trim();
        } else {
            ip = request.getRemoteAddr();
        }

        return ResponseEntity.ok(authService.refresh(req.refreshToken, ip));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest req) {
        authService.logout(req.refreshToken);
        return ResponseEntity.noContent().build();
    }

}
