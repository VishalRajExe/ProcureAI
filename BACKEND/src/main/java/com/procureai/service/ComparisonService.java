package com.procureai.service;

import com.procureai.entity.Quote;
import com.procureai.repository.QuoteRepository;
import com.procureai.service.ai.AIProvider;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ComparisonService {

    private final QuoteRepository quoteRepository;
    private final ScoringService scoringService;
    private final AIProvider aiProvider;

    public ComparisonService(QuoteRepository quoteRepository, ScoringService scoringService, AIProvider aiProvider) {
        this.quoteRepository = quoteRepository;
        this.scoringService = scoringService;
        this.aiProvider = aiProvider;
    }

    public record ComparisonResult(List<Quote> rankedQuotes, Quote recommended, String aiExplanation) {}

    public ComparisonResult compare(Long workflowId, ScoringService.Weights weights) {
        List<Quote> quotes = quoteRepository.findByWorkflowId(workflowId).stream()
                .filter(q -> q.getExtractionStatus() == Quote.ExtractionStatus.VALIDATED)
                .toList();

        if (quotes.isEmpty()) {
            return new ComparisonResult(List.of(), null, "No successfully validated quotes available for comparison yet.");
        }

        scoringService.scoreAll(quotes, weights == null ? ScoringService.Weights.DEFAULT : weights);
        quoteRepository.saveAll(quotes);

        List<Quote> ranked = quotes.stream()
                .sorted(Comparator.comparingDouble((Quote q) -> q.getVendorScore() == null ? 0 : q.getVendorScore()).reversed())
                .toList();

        Quote recommended = ranked.get(0);
        String summary = buildSummary(ranked, recommended);
        String explanation = aiProvider.explainRecommendation(recommended, summary);

        return new ComparisonResult(ranked, recommended, explanation);
    }

    private String buildSummary(List<Quote> ranked, Quote recommended) {
        StringBuilder sb = new StringBuilder();
        sb.append("Compared ").append(ranked.size()).append(" vendors. ");
        sb.append(recommended.getVendor().getName())
                .append(" scored ").append(recommended.getVendorScore())
                .append("/100 with actual total ₹").append(recommended.getCalculatedTotal())
                .append(", warranty ").append(recommended.getWarrantyMonths()).append(" months, delivery ")
                .append(recommended.getDeliveryDays()).append(" days, benchmark status ")
                .append(recommended.getBenchmarkStatus()).append(".");
        return sb.toString();
    }
}
