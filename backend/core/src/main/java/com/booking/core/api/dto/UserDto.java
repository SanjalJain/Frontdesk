package com.booking.core.api.dto;

import java.time.Instant;

public record UserDto(
        Long id,
        Long tenantId,
        String email,
        String fullName,
        String role,
        boolean isActive,
        Instant lastLoginAt
) {}
