package com.booking.business.api.dto;

import java.time.OffsetDateTime;

public record AvailableSlotDto(
        Long staffId,
        String staffName,
        String staffColor,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {}
