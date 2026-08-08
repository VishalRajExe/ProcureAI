package com.procureai.dto;

import jakarta.validation.constraints.NotNull;

public record NegotiationApprovalRequest(
        @NotNull Boolean approve,
        String editedEmailBody, // optional — human can edit AI draft before sending
        String notes
) {}
