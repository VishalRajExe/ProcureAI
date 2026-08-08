package com.procureai.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT generation and validation service.
 *
 * Security:
 * - Algorithm: HS256 with minimum 32-byte key enforced at startup
 * - Claims: email (subject), role, userId — no sensitive data included
 * - Expiry: configurable via app.jwt.expiration-ms
 * - Default dev secret triggers startup WARNING — must be replaced in production
 * - Tokens are validated on every request via JwtAuthFilter
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String DEV_SECRET_MARKER = "CHANGE_ME";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiration-ms}") long expirationMs) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

        // Enforce minimum key length for HS256
        if (bytes.length < 32) {
            throw new IllegalStateException(
                "app.jwt.secret must be at least 32 characters long. " +
                "Set the JWT_SECRET environment variable before deployment.");
        }

        // Warn loudly if the default dev secret is being used
        if (secret.contains(DEV_SECRET_MARKER)) {
            log.warn("=================================================================");
            log.warn("  SECURITY WARNING: Using default development JWT secret!");
            log.warn("  Set JWT_SECRET environment variable before any real deployment.");
            log.warn("=================================================================");
        }

        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT containing the minimum required claims.
     * NEVER include password hashes, sensitive user data, or API keys in tokens.
     */
    public String generateToken(String email, String role, Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("uid", userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public io.jsonwebtoken.Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
