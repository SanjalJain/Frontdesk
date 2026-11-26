package com.booking.business.api.dto;

public record MethodBreakdownDto(
        String method,
        int count,
        long totalPaise
) {}
