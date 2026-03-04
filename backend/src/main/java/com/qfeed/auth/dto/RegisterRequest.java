package com.qfeed.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 32)
    public String username;

    @NotBlank
    @Size(min = 6, max = 64)
    public String password;
}
