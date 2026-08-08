package com.procureai.dto;

import jakarta.validation.constraints.NotNull;

/**
 * @deprecated Use {@link QuoteDtos.NegotiationApprovalRequest} for new code.
 */
@Deprecated
public record NegotiationApprovalRequest(
        @NotNull Boolean approve,
        @jakarta.validation.constraints.Size(max = 10_000) String editedEmailBody,
        @jakarta.validation.constraints.Size(max = 1_000) String notes
) {}
