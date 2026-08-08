package com.procureai.service;

import com.procureai.dto.AuthDtos;
import com.procureai.entity.User;
import com.procureai.repository.UserRepository;
import com.procureai.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

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
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        // Security: role is never trusted blindly from client input beyond the safe subset;
        // ADMIN cannot be self-assigned through public registration.
        User.Role role = User.Role.PROCUREMENT_USER;
        if ("APPROVER".equalsIgnoreCase(req.role())) role = User.Role.APPROVER;
        user.setRole(role);
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return new AuthDtos.AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase())
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
