package com.booking.customer.api.dto;

import java.util.List;
import java.util.Map;

public record PublicTenantProfileDto(
        Long tenantId,
        String slug,
        String name,
        String vertical,
        String timezone,
        String currency,
        int slotMinutes,
        String logoPath,
        List<PublicServiceDto> services,
        List<PublicWorkingHoursDto> hours,
        Map<String, Object> branding
) {}
