package com.booking.business.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record FollowUpCallDto(
        Long appointmentId,
        Long customerId,
        String customerName,
        String customerPhone,
        String customerEmail,
        String serviceName,
        String staffName,
        OffsetDateTime lastVisitAt,
        String reaction,
        String reactionNote,
        String outcome,
        String followupStatus,
        LocalDate followupDueOn
) {}
