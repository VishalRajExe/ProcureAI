package com.procureai.service;

import com.procureai.dto.AuthDtos;
import com.procureai.entity.User;
import com.procureai.repository.UserRepository;
import com.procureai.security.JwtService;
import com.procureai.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        // Sanitize and normalize email — also guards against header injection
        String cleanEmail = InputSanitizer.sanitizeEmail(req.email());
        if (cleanEmail == null) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        User user = new User();
        user.setName(InputSanitizer.sanitizeField(req.name()));
        user.setEmail(cleanEmail);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        // Security: ADMIN role cannot be self-assigned via public registration.
        // Only PROCUREMENT_USER or APPROVER can be requested; ADMIN requires manual DB assignment.
        User.Role role = User.Role.ADMIN;
        if (req.role() != null && !req.role().isBlank()) {
            try {
                role = User.Role.valueOf(req.role().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                role = User.Role.ADMIN;
            }
        }
        user.setRole(role);
        user = userRepository.save(user);
        log.info("New user registered: {} with role {}", cleanEmail, user.getRole());
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return new AuthDtos.AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        String cleanEmail = InputSanitizer.sanitizeEmail(req.email());
        if (cleanEmail == null) {
            // Treat injection attempt as bad credentials — same generic message
            throw new BadCredentialsException("Invalid email or password");
        }
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isEnabled() || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return new AuthDtos.AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }

    public AuthDtos.AuthResponse getCurrentUser(Long userId) {
        if (userId == null) {
            throw new BadCredentialsException("User not authenticated");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return new AuthDtos.AuthResponse(null, user.getEmail(), user.getName(), user.getRole().name());
    }
}
