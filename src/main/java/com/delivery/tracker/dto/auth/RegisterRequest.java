package com.delivery.tracker.dto.auth;

import com.delivery.tracker.util.EmailNormalizer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

public record RegisterRequest(@NotBlank @Size(max=160) String name, @NotBlank @Email @Size(max=254) String email, @NotBlank String password) {
    public RegisterRequest { email=EmailNormalizer.normalize(email); }
}
