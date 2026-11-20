package com.booking.business.api.dto;

import java.time.Instant;
import java.time.LocalDate;

public record CustomerDto(
        Long id,
        Long tenantId,
        String fullName,
        String phoneE164,
        String email,
        LocalDate dob,
        String notes,
        String intakeJson,
        Instant createdAt
) {}
