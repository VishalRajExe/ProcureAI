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

import com.procureai.service.email.EmailService;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
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
        return new AuthDtos.AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole().name(), getPermissionsForRole(user.getRole()));
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
        return new AuthDtos.AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole().name(), getPermissionsForRole(user.getRole()));
    }

    public AuthDtos.AuthResponse getCurrentUser(Long userId) {
        if (userId == null) {
            throw new BadCredentialsException("User not authenticated");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return new AuthDtos.AuthResponse(null, user.getId(), user.getEmail(), user.getName(), user.getRole().name(), getPermissionsForRole(user.getRole()));
    }

    @Transactional
    public void forgotPassword(AuthDtos.ForgotPasswordRequest req) {
        String cleanEmail = InputSanitizer.sanitizeEmail(req.email());
        if (cleanEmail == null) {
            throw new IllegalArgumentException("Invalid email address");
        }
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new com.procureai.exception.NotFoundException("No account found with email address: " + cleanEmail));

        // Generate 6-digit verification code OTP
        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        user.setResetOtp(otp);
        user.setResetOtpExpiry(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        String subject = "ProcureAI Password Reset Verification Code: " + otp;
        String body = "Dear " + user.getName() + ",\n\n"
                + "You have requested a password reset for your ProcureAI Enterprise Procurement Platform account.\n\n"
                + "Your 6-digit verification code (OTP) is:\n\n"
                + "   " + otp + "\n\n"
                + "This verification code will expire in 15 minutes.\n"
                + "If you did not request a password reset, please ignore this email.\n\n"
                + "Best regards,\n"
                + "ProcureAI Security Team";

        log.info("Sending password reset verification code (OTP) via Brevo/EmailService to {}", cleanEmail);
        emailService.sendEmailDetails(cleanEmail, subject, body, null, null);
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest req) {
        String cleanEmail = InputSanitizer.sanitizeEmail(req.email());
        if (cleanEmail == null) {
            throw new IllegalArgumentException("Invalid email address");
        }
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new com.procureai.exception.NotFoundException("No account found with email address: " + cleanEmail));

        if (user.getResetOtp() == null || user.getResetOtpExpiry() == null
                || !user.getResetOtp().trim().equals(req.otp().trim())
                || java.time.LocalDateTime.now().isAfter(user.getResetOtpExpiry())) {
            throw new IllegalArgumentException("Invalid or expired verification code (OTP). Please request a new code.");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);
        log.info("Password successfully reset for user {}", cleanEmail);
    }

    private java.util.List<String> getPermissionsForRole(User.Role role) {
        if (role == null) return java.util.List.of("READ");
        return switch (role) {
            case ADMIN -> java.util.List.of("READ", "WRITE", "APPROVE", "DELETE", "MANAGE_USERS", "EXECUTE_DEMO");
            case APPROVER -> java.util.List.of("READ", "WRITE", "APPROVE");
            case PROCUREMENT_USER -> java.util.List.of("READ", "WRITE");
            case VIEWER -> java.util.List.of("READ");
        };
    }
}
