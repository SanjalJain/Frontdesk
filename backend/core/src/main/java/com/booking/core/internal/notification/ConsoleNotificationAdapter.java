package com.booking.core.internal.notification;

import com.booking.core.api.notification.NotificationMessage;
import com.booking.core.api.notification.NotificationResult;
import com.booking.core.api.notification.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ConsoleNotificationAdapter implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificationAdapter.class);

    @Override
    public NotificationResult send(NotificationMessage message) {
        String msgId = "mock-wa-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("""
                
                ================== [MOCK NOTIFICATION SENDER] ==================
                To: {} ({})
                Channel: {} | Kind: {} | TenantId: {}
                Provider Message ID: {}
                --------------------------- MESSAGE ---------------------------
                {}
                ================================================================
                """,
                message.recipientPhone(),
                message.recipientName() != null ? message.recipientName() : "Unknown",
                message.channel().toUpperCase(),
                message.kind(),
                message.tenantId(),
                msgId,
                message.body()
        );

        return NotificationResult.ok(msgId);
    }

    @Override
    public String providerName() {
        return "mock";
    }
}
