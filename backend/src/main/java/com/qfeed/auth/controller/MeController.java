package com.qfeed.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        Object username = request.getAttribute("username");

        return ResponseEntity.ok(new Object() {
            public final Object uid = userId;
            public final Object name = username;
        });
    }
}
