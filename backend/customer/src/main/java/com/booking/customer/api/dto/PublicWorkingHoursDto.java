package com.booking.customer.api.dto;

public record PublicWorkingHoursDto(
        int weekday,
        String startTime,
        String endTime
) {}
