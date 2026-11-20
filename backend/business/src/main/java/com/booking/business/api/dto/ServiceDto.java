package com.booking.business.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ServiceDto(
        Long id,
        Long tenantId,
        String name,
        int durationMin,
        int bufferAfterMin,
        int pricePaise,
        boolean isActive
) {
    public record CreateServiceRequest(
            @NotBlank(message = "Service name is required")
            String name,

            @Positive(message = "Duration must be greater than zero")
            int durationMin,

            @Min(value = 0, message = "Buffer must be non-negative")
            int bufferAfterMin,

            @Min(value = 0, message = "Price cannot be negative")
            int pricePaise
    ) {}
}
