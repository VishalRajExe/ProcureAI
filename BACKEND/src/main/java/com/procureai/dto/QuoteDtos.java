package com.procureai.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request DTOs for the financial quote submission endpoint.
 */
public class QuoteDtos {

    /**
     * JSON-based quote submission.
     */
    public record QuoteUploadRequest(
            @NotBlank(message = "Vendor name is required")
            @Size(min = 1, max = 200, message = "Vendor name must be between 1 and 200 characters")
            String vendorName,

            @Email(message = "Vendor email must be a valid email address")
            @Size(max = 254, message = "Vendor email must not exceed 254 characters")
            String vendorEmail,

            String rawDocumentText,

            String rawText,

            @Size(max = 255, message = "Source file name must not exceed 255 characters")
            String sourceFileName
    ) {
        public String getEffectiveRawText() {
            if (rawDocumentText != null && !rawDocumentText.isBlank()) {
                return rawDocumentText;
            }
            return rawText != null ? rawText : "";
        }
    }

    public record CreateWorkflowRequest(
            @NotBlank(message = "Workflow title is required")
            @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
            String title,

            @Size(max = 1000, message = "Description must not exceed 1000 characters")
            String description
    ) {}

    public record NegotiationApprovalRequest(
            @NotNull(message = "approve is required — must be true or false")
            Boolean approve,

            @Size(max = 10_000, message = "Email body must not exceed 10,000 characters")
            String editedEmailBody,

            @Email(message = "Recipient email must be a valid email address")
            @Size(max = 254, message = "Recipient email must not exceed 254 characters")
            String recipientEmail,

            @Size(max = 1000, message = "Notes must not exceed 1000 characters")
            String notes
    ) {}

    public record VendorResponseRequest(
            @NotNull(message = "counterPrice is required")
            @DecimalMin(value = "0.01", message = "Counter price must be greater than zero")
            @DecimalMax(value = "1000000000", message = "Counter price exceeds maximum allowed value")
            @Digits(integer = 12, fraction = 2, message = "Counter price has invalid format")
            BigDecimal counterPrice
    ) {}

    public record ScoringWeightsRequest(
            @DecimalMin("0.0") @DecimalMax("1.0") Double price,
            @DecimalMin("0.0") @DecimalMax("1.0") Double warranty,
            @DecimalMin("0.0") @DecimalMax("1.0") Double delivery,
            @DecimalMin("0.0") @DecimalMax("1.0") Double reliability,
            @DecimalMin("0.0") @DecimalMax("1.0") Double compliance
    ) {}
}
