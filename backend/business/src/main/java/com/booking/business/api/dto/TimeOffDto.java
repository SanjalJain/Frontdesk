package com.booking.business.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record TimeOffDto(
        Long id,
        Long tenantId,
        Long staffId,
        Instant startsAt,
        Instant endsAt,
        String reason
) {
    public record CreateTimeOffRequest(
            Long staffId, // nullable = entire business is closed

            @NotNull(message = "Start timestamp is required")
            Instant startsAt,

            @NotNull(message = "End timestamp is required")
            Instant endsAt,

            String reason
    ) {}
}
