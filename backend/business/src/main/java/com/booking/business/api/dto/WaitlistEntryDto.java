package com.booking.business.api.dto;

import java.time.Instant;
import java.time.LocalDate;

public record WaitlistEntryDto(
        Long id,
        Long tenantId,
        Long customerId,
        String customerName,
        String customerPhone,
        Long serviceId,
        String serviceName,
        Long staffId,
        String staffName,
        LocalDate earliestDate,
        LocalDate latestDate,
        String notes,
        String status,
        Instant createdAt
) {}
