package com.booking.business.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AppointmentDto(
        Long id,
        Long tenantId,
        Long customerId,
        String customerName,
        String customerPhone,
        Long serviceId,
        String serviceName,
        int serviceDurationMin,
        Long staffId,
        String staffName,
        String staffColor,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String status,
        String source,
        String requestNote,
        int pricePaise,
        int version,
        OffsetDateTime checkedInAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String outcome,
        String noShowReason,
        String cancelReason,
        String reaction,
        String reactionNote,
        String followupStatus,
        LocalDate followupDueOn,
        Instant createdAt,
        Instant updatedAt
) {}
