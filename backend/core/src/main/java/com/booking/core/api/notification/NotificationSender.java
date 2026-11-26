package com.booking.core.api.notification;

public interface NotificationSender {
    NotificationResult send(NotificationMessage message);
    String providerName();
}
