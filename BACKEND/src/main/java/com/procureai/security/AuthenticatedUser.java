package com.procureai.security;

public record AuthenticatedUser(Long id, String email, String role) {
}
