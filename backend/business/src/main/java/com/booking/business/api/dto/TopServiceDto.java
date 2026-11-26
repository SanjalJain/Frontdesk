package com.booking.business.api.dto;

public record TopServiceDto(
        Long serviceId,
        String serviceName,
        int bookingCount,
        long revenuePaise
) {}
