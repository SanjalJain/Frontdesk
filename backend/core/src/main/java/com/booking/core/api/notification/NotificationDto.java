package com.booking.core.api.notification;

import java.time.Instant;
import java.time.OffsetDateTime;

public record NotificationDto(
        Long id,
        Long tenantId,
        String refType,
        Long refId,
        String customerPhone,
        String customerName,
        String kind,
        String channel,
        String provider,
        String body,
        OffsetDateTime scheduledFor,
        String status,
        int attempts,
        OffsetDateTime nextAttemptAt,
        String lastError,
        String providerMessageId,
        OffsetDateTime sentAt,
        String dedupeKey,
        Instant createdAt
) {}
