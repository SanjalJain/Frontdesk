package com.booking.core.internal.notification;

import com.booking.core.api.notification.NotificationDto;
import com.booking.core.api.notification.NotificationMessage;
import com.booking.core.api.notification.NotificationResult;
import com.booking.core.api.notification.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class NotificationWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationWorker.class);

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;

    public NotificationWorker(
            NotificationRepository notificationRepository,
            NotificationSender notificationSender
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationSender = notificationSender;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processDueNotifications() {
        List<NotificationDto> dueNotifications = notificationRepository.fetchDueNotifications(20);
        if (dueNotifications.isEmpty()) {
            return;
        }

        log.info("Processing {} due notifications...", dueNotifications.size());

        for (NotificationDto n : dueNotifications) {
            try {
                NotificationMessage msg = new NotificationMessage(
                        n.tenantId(),
                        n.customerPhone(),
                        n.customerName(),
                        n.channel(),
                        n.body(),
                        n.kind()
                );

                NotificationResult result = notificationSender.send(msg);

                if (result.success()) {
                    log.info("Notification id={} sent successfully via {}", n.id(), notificationSender.providerName());
                    notificationRepository.markSent(n.id(), result.providerMessageId());
                } else {
                    int nextAttempt = n.attempts() + 1;
                    OffsetDateTime nextAttemptAt = calculateNextAttempt(nextAttempt);
                    log.warn("Notification id={} failed on attempt {}: {}", n.id(), nextAttempt, result.errorMessage());
                    notificationRepository.markFailed(n.id(), result.errorMessage(), nextAttempt, nextAttemptAt);
                }
            } catch (Exception e) {
                int nextAttempt = n.attempts() + 1;
                OffsetDateTime nextAttemptAt = calculateNextAttempt(nextAttempt);
                log.error("Exception delivering notification id={}: {}", n.id(), e.getMessage(), e);
                notificationRepository.markFailed(n.id(), e.getMessage(), nextAttempt, nextAttemptAt);
            }
        }
    }

    private OffsetDateTime calculateNextAttempt(int attempt) {
        long delayMinutes = switch (attempt) {
            case 1 -> 1;
            case 2 -> 5;
            default -> 15;
        };
        return OffsetDateTime.now().plusMinutes(delayMinutes);
    }
}
