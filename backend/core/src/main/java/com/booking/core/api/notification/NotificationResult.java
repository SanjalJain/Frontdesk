package com.booking.core.api.notification;

public record NotificationResult(
        boolean success,
        String providerMessageId,
        String errorMessage
) {
    public static NotificationResult ok(String providerMessageId) {
        return new NotificationResult(true, providerMessageId, null);
    }

    public static NotificationResult failure(String errorMessage) {
        return new NotificationResult(false, null, errorMessage);
    }
}
