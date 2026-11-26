package com.booking.business.api.dto;

public record StaffPerformanceDto(
        Long staffId,
        String staffName,
        int totalBookings,
        int completedBookings,
        long revenuePaise
) {}
