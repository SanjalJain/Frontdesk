package com.booking.business.api.dto;

import java.time.OffsetDateTime;

public record TrackerAppointmentDto(
        Long id,
        Long customerId,
        String customerName,
        String customerPhone,
        Long serviceId,
        String serviceName,
        Long staffId,
        String staffName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String status,
        int pricePaise,
        int paidPaise,
        long customerBalancePaise,
        String reaction,
        String outcome,
        String note
) {}
