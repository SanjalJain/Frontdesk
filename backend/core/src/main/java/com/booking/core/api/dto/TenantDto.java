package com.booking.core.api.dto;

import java.time.Instant;

public record TenantDto(
        Long id,
        String slug,
        String name,
        String vertical,
        String timezone,
        String currency,
        int slotMinutes,
        String settingsJson,
        String logoPath,
        boolean isActive,
        Instant createdAt
) {}
