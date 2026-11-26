package com.booking.customer.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record SubmitInboundRequest(
        @NotBlank(message = "type is required")
        @Pattern(regexp = "appointment|call_back", message = "type must be appointment or call_back")
        String type,

        @NotBlank(message = "customerName is required")
        String customerName,

        @NotBlank(message = "phone is required")
        String phone,

        String email,

        Long serviceId,

        LocalDate preferredDate,

        String preferredTimeWindow,

        String concern,

        // Honeypot anti-bot trap field (must remain empty)
        String hp_comment
) {}
