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
        seedBenchmark("Business Laptop", "60000", "72000");
        seedBenchmark("HP EliteBook", "55000", "75000");
        seedBenchmark("Dell Latitude", "62000", "82000");
        seedBenchmark("Lenovo ThinkPad", "52000", "70000");
        seedBenchmark("Server", "150000", "500000");
        seedBenchmark("Office Furniture", "8000", "35000");
        seedBenchmark("Software License", "2000", "40000");
        seedBenchmark("Displays & TVs", "40000", "160000");
        seedBenchmark("LG 55-inch OLED TV", "90000", "150000");

        seedUser("admin@procureai.demo", "Admin User", "Admin@12345", User.Role.ADMIN);
        seedUser("approver@procureai.demo", "Approver User", "Approver@12345", User.Role.APPROVER);
        seedUser("viewer@procureai.demo", "Procurement Viewer", "Viewer@12345", User.Role.VIEWER);
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

    private void seedBenchmark(String category, String min, String max) {
        if (benchmarkRepository.findFirstByProductCategoryIgnoreCase(category).isEmpty()) {
            Benchmark b = new Benchmark();
            b.setProductCategory(category);
            b.setReferenceMinUnitPrice(new BigDecimal(min));
            b.setReferenceMaxUnitPrice(new BigDecimal(max));
            b.setSource("ProcureAI Market Intelligence — Seeded Reference Data");
            benchmarkRepository.save(b);
        }
    }
}
