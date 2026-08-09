package com.procureai.dto;

import jakarta.validation.constraints.*;

public class AuthDtos {

    /**
     * Registration request with hardened validation.
     *
     * Security rules:
     * - name: max 100 chars, no HTML/control characters
     * - email: RFC-compliant format, max 254 chars (RFC 5321)
     * - password: min 8, max 128 chars; at least 1 uppercase, 1 digit, 1 special char
     * - role: not trusted — silently ignored; role assignment done server-side
     */
    public record RegisterRequest(
            @NotBlank(message = "Name is required")
            @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
            @Pattern(regexp = "^[\\p{L}\\p{N} .,'\\-]+$",
                     message = "Name contains invalid characters")
            String name,

            @Email(message = "A valid email address is required")
            @NotBlank(message = "Email is required")
            @Size(max = 254, message = "Email must not exceed 254 characters")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
            @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
                message = "Password must contain at least one uppercase letter, one digit, and one special character"
            )
            String password,

            // role is intentionally not validated here — the service layer ignores
            // any self-assigned ADMIN/APPROVER role from public registration
            String role
    ) {}

    /**
     * Login request. Validation exists to prevent trivially malformed input
     * before the expensive password-hash comparison runs.
     */
    public record LoginRequest(
            @Email(message = "A valid email address is required")
            @NotBlank(message = "Email is required")
            @Size(max = 254)
            String email,

            @NotBlank(message = "Password is required")
            @Size(max = 128, message = "Password too long")
            String password
    ) {}

    /**
     * Auth response — never includes passwordHash, internal IDs beyond what the
     * frontend strictly needs, or any server-side secrets.
     */
    public record AuthResponse(
            String token,
            Long id,
            String email,
            String name,
            String role,
            java.util.List<String> permissions
    ) {}
}
