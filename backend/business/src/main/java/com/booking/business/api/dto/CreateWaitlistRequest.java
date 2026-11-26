package com.booking.business.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateWaitlistRequest(
        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Service ID is required")
        Long serviceId,

        Long staffId,

        LocalDate earliestDate,
        LocalDate latestDate,
        String notes
) {}
