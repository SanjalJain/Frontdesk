package com.booking.business.api.dto;

public record CustomerBalanceDto(
        Long customerId,
        long billedPaise,
        long paidPaise,
        long balancePaise
) {}
