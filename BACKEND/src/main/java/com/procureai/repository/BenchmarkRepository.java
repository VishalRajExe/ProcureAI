package com.procureai.repository;

import com.procureai.entity.Benchmark;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BenchmarkRepository extends JpaRepository<Benchmark, Long> {
    Optional<Benchmark> findFirstByProductCategoryIgnoreCase(String category);
}
