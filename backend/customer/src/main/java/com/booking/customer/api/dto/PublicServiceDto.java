package com.booking.customer.api.dto;

public record PublicServiceDto(
        Long id,
        String name,
        int durationMin,
        int pricePaise
) {}
