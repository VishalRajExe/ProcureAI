package com.procureai.service;

import com.procureai.entity.Benchmark;
import com.procureai.entity.Quote;
import com.procureai.repository.BenchmarkRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Curated reference/demo pricing lookups. Explicitly NOT live market data — every
 * benchmark record is labeled "Reference/Demo Benchmark Data" (see Benchmark entity).
 * Structured so a real pricing API can populate/replace the underlying data source
 * later without changing any caller of this service.
 */
@Service
public class BenchmarkService {

    private static final String DEFAULT_CATEGORY = "Business Laptop";

    private final BenchmarkRepository benchmarkRepository;

    public BenchmarkService(BenchmarkRepository benchmarkRepository) {
        this.benchmarkRepository = benchmarkRepository;
    }

    public Optional<Benchmark> findForCategory(String category) {
        String key = (category == null || category.isBlank()) ? DEFAULT_CATEGORY : category;
        return benchmarkRepository.findFirstByProductCategoryIgnoreCase(key);
    }

    /** Applies the benchmark range to a quote's per-unit price and sets its benchmark status. */
    public void applyBenchmark(Quote quote, BigDecimal unitPrice) {
        Optional<Benchmark> benchmark = findForCategory(DEFAULT_CATEGORY);
        if (benchmark.isEmpty()) {
            quote.setBenchmarkStatus(Quote.BenchmarkStatus.UNKNOWN);
            return;
        }
        Benchmark b = benchmark.get();
        if (unitPrice.compareTo(b.getReferenceMinUnitPrice()) < 0) {
            quote.setBenchmarkStatus(Quote.BenchmarkStatus.BELOW);
        } else if (unitPrice.compareTo(b.getReferenceMaxUnitPrice()) > 0) {
            quote.setBenchmarkStatus(Quote.BenchmarkStatus.ABOVE);
        } else {
            quote.setBenchmarkStatus(Quote.BenchmarkStatus.WITHIN);
        }
    }
}
