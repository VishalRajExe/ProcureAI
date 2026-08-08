package com.procureai.config;

import com.procureai.entity.Benchmark;
import com.procureai.entity.User;
import com.procureai.repository.BenchmarkRepository;
import com.procureai.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds the minimum data needed for a reliable, zero-setup demo: a curated benchmark
 * price range (clearly labeled as reference/demo data) and two ready-to-use demo
 * accounts. Idempotent — safe to run on every startup.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final BenchmarkRepository benchmarkRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(BenchmarkRepository benchmarkRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.benchmarkRepository = benchmarkRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (benchmarkRepository.findFirstByProductCategoryIgnoreCase("Business Laptop").isEmpty()) {
            Benchmark b = new Benchmark();
            b.setProductCategory("Business Laptop");
            b.setReferenceMinUnitPrice(new BigDecimal("60000"));
            b.setReferenceMaxUnitPrice(new BigDecimal("70000"));
            b.setSource("Reference/Demo Benchmark Data");
            benchmarkRepository.save(b);
        }

        seedUser("admin@procureai.demo", "Admin User", "Admin@12345", User.Role.ADMIN);
        seedUser("approver@procureai.demo", "Approver User", "Approver@12345", User.Role.APPROVER);
    }

    private void seedUser(String email, String name, String rawPassword, User.Role role) {
        if (userRepository.existsByEmail(email)) return;
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);
    }
}
