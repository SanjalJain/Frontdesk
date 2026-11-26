package com.booking.business.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record RescheduleAppointmentRequest(
        @NotNull(message = "New start time is required")
        OffsetDateTime startsAt,

        Long staffId, // optional new staffId; if null, keeps current staff

        @NotNull(message = "Appointment version is required for optimistic locking")
        Integer version
) {}
