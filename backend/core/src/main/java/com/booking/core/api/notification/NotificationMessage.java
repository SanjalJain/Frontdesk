package com.booking.core.api.notification;

public record NotificationMessage(
        Long tenantId,
        String recipientPhone,
        String recipientName,
        String channel, // 'whatsapp', 'sms'
        String body,
        String kind
) {}
