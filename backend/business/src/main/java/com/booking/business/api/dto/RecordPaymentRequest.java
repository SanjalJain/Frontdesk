package com.booking.business.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RecordPaymentRequest(
        @NotNull(message = "customerId is required")
        Long customerId,

        Long appointmentId,

        @NotNull(message = "amountPaise is required")
        @Min(value = 1, message = "amountPaise must be greater than 0")
        Integer amountPaise,

        @NotBlank(message = "method is required")
        @Pattern(regexp = "cash|upi|card", message = "method must be cash, upi, or card")
        String method,

        String reference,

        String note
) {}
