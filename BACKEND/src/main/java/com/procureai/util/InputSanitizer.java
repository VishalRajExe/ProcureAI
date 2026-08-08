package com.procureai.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Input sanitization utilities for untrusted document text that will be
 * processed by AI providers.
 *
 * Security purpose:
 * - Prevents prompt injection via oversized inputs
 * - Strips null bytes and control characters that could confuse parsers
 * - Enforces maximum text length to prevent resource exhaustion
 * - Does NOT attempt HTML escaping (not applicable for AI text pipelines)
 *
 * AI Security Model:
 * - All document text is treated as UNTRUSTED input, even after OCR
 * - AI output is NEVER directly executed — it goes through schema validation
 *   and business-rule enforcement before any action is taken
 * - See NegotiationService and QuoteService for backend enforcement logic
 */
public final class InputSanitizer {

    private static final Logger log = LoggerFactory.getLogger(InputSanitizer.class);

    /** Maximum characters sent to the AI provider to prevent resource exhaustion */
    public static final int AI_TEXT_MAX_CHARS = 50_000;

    /** Maximum characters for short string fields (names, terms, etc.) */
    public static final int FIELD_MAX_CHARS = 500;

    private InputSanitizer() {}

    /**
     * Sanitizes raw document text before passing to an AI provider.
     *
     * - Truncates to {@link #AI_TEXT_MAX_CHARS}
     * - Strips null bytes (which can confuse parsers and bypass filtering)
     * - Strips ASCII control characters (except newline, carriage-return, tab)
     * - Collapses excessive blank lines (>3 consecutive) to reduce padding attacks
     */
    public static String sanitizeForAi(String rawText) {
        if (rawText == null) return "";

        // Strip null bytes and dangerous control characters
        String cleaned = rawText
                .replace("\u0000", "")   // null byte
                .replaceAll("[\\x01-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", " "); // control chars except \t \n \r

        // Collapse 3+ consecutive blank lines to prevent excessive padding
        cleaned = cleaned.replaceAll("(\r?\n){4,}", "\n\n\n");

        if (cleaned.length() > AI_TEXT_MAX_CHARS) {
            log.warn("AI input text truncated from {} to {} characters to prevent resource exhaustion",
                    cleaned.length(), AI_TEXT_MAX_CHARS);
            cleaned = cleaned.substring(0, AI_TEXT_MAX_CHARS);
        }

        return cleaned;
    }

    /**
     * Sanitizes a vendor name or short text field.
     * - Strips leading/trailing whitespace
     * - Limits to {@link #FIELD_MAX_CHARS}
     */
    public static String sanitizeField(String value) {
        if (value == null) return null;
        String stripped = value.strip();
        return stripped.length() > FIELD_MAX_CHARS ? stripped.substring(0, FIELD_MAX_CHARS) : stripped;
    }

    /**
     * Validates an email address does not contain header injection characters.
     * Returns the sanitized email or null if unsafe.
     */
    public static String sanitizeEmail(String email) {
        if (email == null) return null;
        String cleaned = email.strip().toLowerCase();
        // Email header injection characters
        if (cleaned.contains("\n") || cleaned.contains("\r") || cleaned.contains("%0a") || cleaned.contains("%0d")) {
            log.warn("Rejected email with injection characters");
            return null;
        }
        return cleaned.length() > 254 ? null : cleaned;
    }
}
