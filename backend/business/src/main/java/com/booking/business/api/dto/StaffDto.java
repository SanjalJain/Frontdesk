package com.booking.business.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record StaffDto(
        Long id,
        Long tenantId,
        String name,
        String title,
        String color,
        boolean isBookable,
        boolean isActive,
        int sortOrder,
        List<Long> serviceIds
) {
    public record CreateStaffRequest(
            @NotBlank(message = "Staff name is required")
            String name,

            String title,
            String color,
            Boolean isBookable,
            Integer sortOrder,
            List<Long> serviceIds
    ) {}
}
