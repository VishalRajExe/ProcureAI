package com.procureai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * JSON-based quote submission (used by the demo and by clients that already have
 * structured data). File uploads (PDF/image) go through the separate multipart
 * endpoint, which runs OCR + AI extraction and then normalizes into the same
 * downstream pipeline.
 */
public record QuoteUploadRequest(
        @NotBlank String vendorName,
        String vendorEmail,
        @NotBlank String rawDocumentText, // raw text content to be parsed (mirrors what OCR would produce)
        String sourceFileName
) {}
