package com.booking.core.api.notification;

public record MessageTemplateDto(
        Long id,
        Long tenantId,
        String kind,
        String channel,
        String language,
        String body,
        boolean isActive
) {}
