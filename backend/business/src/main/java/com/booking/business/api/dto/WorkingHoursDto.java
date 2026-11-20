package com.booking.business.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record WorkingHoursDto(
        Long id,
        Long tenantId,
        Long staffId,
        int weekday,
        LocalTime startTime,
        LocalTime endTime
) {
    public record WorkingHoursRequest(
            @NotNull(message = "Staff ID is required")
            Long staffId,

            @Min(1) @Max(7)
            int weekday,

            @NotNull(message = "Start time is required")
            LocalTime startTime,

            @NotNull(message = "End time is required")
            LocalTime endTime
    ) {}
}
