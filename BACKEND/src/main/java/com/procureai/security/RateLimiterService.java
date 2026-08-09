package com.procureai.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, high-performance in-memory Token Bucket Rate Limiter.
 * Supports per-IP and per-user limits across auth, AI, upload, email, and PO endpoints.
 */
@Component
public class RateLimiterService {

    @Value("${app.rate-limit.login:10}")
    private int loginLimitPerMinute;

    @Value("${app.rate-limit.ai:30}")
    private int aiLimitPerMinute;

    @Value("${app.rate-limit.email:10}")
    private int emailLimitPerMinute;

    @Value("${app.rate-limit.upload:15}")
    private int uploadLimitPerMinute;

    @Value("${app.rate-limit.po:20}")
    private int poLimitPerMinute;

    private static class TokenBucket {
        final int capacity;
        double tokens;
        long lastRefillTimestamp;

        TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        synchronized boolean tryConsume(int limitPerMinute) {
            long now = System.currentTimeMillis();
            double refillRatePerMs = (double) limitPerMinute / 60000.0;
            long elapsed = now - lastRefillTimestamp;
            tokens = Math.min(capacity, tokens + elapsed * refillRatePerMs);
            lastRefillTimestamp = now;

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        synchronized int getRemainingTokens(int limitPerMinute) {
            long now = System.currentTimeMillis();
            double refillRatePerMs = (double) limitPerMinute / 60000.0;
            long elapsed = now - lastRefillTimestamp;
            double current = Math.min(capacity, tokens + elapsed * refillRatePerMs);
            return (int) Math.floor(current);
        }
    }

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public enum EndpointCategory {
        LOGIN, AI, EMAIL, UPLOAD, PO, DEFAULT
    }

    public static class RateLimitResult {
        private final boolean allowed;
        private final int limit;
        private final int remaining;
        private final int retryAfterSeconds;

        public RateLimitResult(boolean allowed, int limit, int remaining, int retryAfterSeconds) {
            this.allowed = allowed;
            this.limit = limit;
            this.remaining = remaining;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public boolean isAllowed() { return allowed; }
        public int getLimit() { return limit; }
        public int getRemaining() { return remaining; }
        public int getRetryAfterSeconds() { return retryAfterSeconds; }
    }

    public RateLimitResult checkLimit(String key, EndpointCategory category) {
        int limit = getLimitForCategory(category);
        String bucketKey = category.name() + ":" + key;

        TokenBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new TokenBucket(limit));
        boolean allowed = bucket.tryConsume(limit);
        int remaining = bucket.getRemainingTokens(limit);
        int retryAfter = allowed ? 0 : 60;

        return new RateLimitResult(allowed, limit, remaining, retryAfter);
    }

    private int getLimitForCategory(EndpointCategory category) {
        return switch (category) {
            case LOGIN -> loginLimitPerMinute;
            case AI -> aiLimitPerMinute;
            case EMAIL -> emailLimitPerMinute;
            case UPLOAD -> uploadLimitPerMinute;
            case PO -> poLimitPerMinute;
            case DEFAULT -> 60;
        };
    }

    public EndpointCategory resolveCategory(String path, String method) {
        String lowerPath = path.toLowerCase();
        if (lowerPath.contains("/api/auth/login") || lowerPath.contains("/api/auth/register")) {
            return EndpointCategory.LOGIN;
        }
        if (lowerPath.contains("/upload")) {
            return EndpointCategory.UPLOAD;
        }
        if (lowerPath.contains("/ai/") || lowerPath.contains("/comparison") || lowerPath.contains("/draft")) {
            return EndpointCategory.AI;
        }
        if (lowerPath.contains("/send-email") || lowerPath.contains("/approve") || lowerPath.contains("/retry")) {
            return EndpointCategory.EMAIL;
        }
        if (lowerPath.contains("/purchase-orders/generate")) {
            return EndpointCategory.PO;
        }
        return EndpointCategory.DEFAULT;
    }
}
