package com.booking.business.api.dto;

import java.time.LocalDate;

public record DailyMetricDto(
        LocalDate date,
        int totalBookings,
        int completedBookings,
        long revenuePaise
) {}
