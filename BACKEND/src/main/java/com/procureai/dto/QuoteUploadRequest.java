package com.procureai.dto;

/**
 * Retained for backward compatibility — use QuoteDtos.QuoteUploadRequest for new code.
 * @deprecated Use {@link QuoteDtos.QuoteUploadRequest} instead.
 */
@Deprecated
public record QuoteUploadRequest(
        @jakarta.validation.constraints.NotBlank String vendorName,
        String vendorEmail,
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max = 50_000) String rawDocumentText,
        @jakarta.validation.constraints.Size(max = 255) String sourceFileName
) {}
