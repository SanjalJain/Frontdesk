package com.booking.customer.api.dto;

import java.time.Instant;
import java.time.LocalDate;

public record InboundRequestDto(
        Long id,
        Long tenantId,
        String type,
        String customerName,
        String phoneE164,
        String email,
        Long serviceId,
        String serviceName,
        LocalDate preferredDate,
        String preferredTimeWindow,
        String concern,
        String status,
        Long convertedAppointmentId,
        Instant createdAt
) {}
