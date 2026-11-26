package com.booking.business.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateAppointmentRequest(
        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Service ID is required")
        Long serviceId,

        @NotNull(message = "Staff ID is required")
        Long staffId,

        @NotNull(message = "Start time is required")
        OffsetDateTime startsAt,

        String source, // 'phone', 'walk_in', 'web'

        String requestNote,

        Integer pricePaise // optional override; defaults to service.price_paise
) {}
