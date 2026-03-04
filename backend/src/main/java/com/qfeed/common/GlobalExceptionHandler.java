package com.qfeed.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.qfeed.auth.security.TooManyRequestsException;
import com.qfeed.auth.security.UserLockedException;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().isEmpty()
                ? "validation failed"
                : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", msg));
    }

    @ExceptionHandler(UserLockedException.class)
    public ResponseEntity<ApiError> userLocked(UserLockedException e) {
        return ResponseEntity.status(423).body(new ApiError("ACCOUNT_LOCKED", e.getMessage()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> tooMany(TooManyRequestsException e) {
        return ResponseEntity.status(429).body(new ApiError("TOO_MANY_REQUESTS", e.getMessage()));
    }

}
