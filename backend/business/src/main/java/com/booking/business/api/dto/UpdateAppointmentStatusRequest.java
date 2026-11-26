package com.booking.business.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAppointmentStatusRequest(
        @NotBlank(message = "Status is required")
        @Pattern(regexp = "^(booked|confirmed|arrived|in_progress|completed|cancelled|no_show)$",
                message = "Status must be one of: booked, confirmed, arrived, in_progress, completed, cancelled, no_show")
        String status,

        String note,
        String outcome,
        String reaction, // 'happy', 'neutral', 'unhappy'
        String reactionNote,
        String cancelReason,
        String noShowReason,
        Integer version // optional optimistic locking version
) {}
