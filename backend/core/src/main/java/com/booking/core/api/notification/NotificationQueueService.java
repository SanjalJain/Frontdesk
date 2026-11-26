package com.booking.core.api.notification;

import java.time.OffsetDateTime;

public interface NotificationQueueService {

    void scheduleAppointmentNotifications(
            Long tenantId,
            Long appointmentId,
            String customerPhone,
            String customerName,
            String serviceName,
            String staffName,
            OffsetDateTime startsAt,
            String clinicName,
            String appointmentLabel
    );

    void handleAppointmentRescheduled(
            Long tenantId,
            Long appointmentId,
            String customerPhone,
            String customerName,
            String serviceName,
            String staffName,
            OffsetDateTime newStartsAt,
            String clinicName,
            String appointmentLabel
    );

    void handleAppointmentCancelled(Long tenantId, Long appointmentId);
}
