package com.procureai.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * @deprecated Use {@link QuoteDtos.VendorResponseRequest} for new code.
 */
@Deprecated
public record VendorResponseRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "Counter price must be greater than zero")
        @DecimalMax(value = "1000000000", message = "Counter price exceeds maximum allowed value")
        @Digits(integer = 12, fraction = 2)
        BigDecimal counterPrice
) {}
