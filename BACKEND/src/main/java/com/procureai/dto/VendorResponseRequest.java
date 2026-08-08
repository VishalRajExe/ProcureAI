package com.procureai.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Simulates a vendor's reply arriving in the Vendor Inbox Simulator. */
public record VendorResponseRequest(
        @NotNull BigDecimal counterPrice
) {}
