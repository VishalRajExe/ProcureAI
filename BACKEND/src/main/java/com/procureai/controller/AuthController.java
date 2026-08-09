package com.procureai.controller;

import com.procureai.dto.AuthDtos;
import com.procureai.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.AuthResponse> me() {
        return ResponseEntity.ok(authService.getCurrentUser(com.procureai.util.CurrentUser.id()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<java.util.Map<String, String>> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(java.util.Map.of("message", "A 6-digit verification code has been sent to your email."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<java.util.Map<String, String>> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(java.util.Map.of("message", "Your password has been successfully reset. You can now login."));
    }
}
