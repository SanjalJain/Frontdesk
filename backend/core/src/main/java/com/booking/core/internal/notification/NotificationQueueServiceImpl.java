package com.booking.core.internal.notification;

import com.booking.core.api.notification.MessageTemplateDto;
import com.booking.core.api.notification.NotificationQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationQueueServiceImpl implements NotificationQueueService {

    private static final Logger log = LoggerFactory.getLogger(NotificationQueueServiceImpl.class);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final MessageTemplateRepository templateRepository;
    private final NotificationRepository notificationRepository;
    private final TemplateEngine templateEngine;

    public NotificationQueueServiceImpl(
            MessageTemplateRepository templateRepository,
            NotificationRepository notificationRepository,
            TemplateEngine templateEngine
    ) {
        this.templateRepository = templateRepository;
        this.notificationRepository = notificationRepository;
        this.templateEngine = templateEngine;
    }

    @Override
    @Transactional
    public void scheduleAppointmentNotifications(
            Long tenantId,
            Long appointmentId,
            String customerPhone,
            String customerName,
            String serviceName,
            String staffName,
            OffsetDateTime startsAt,
            String clinicName,
            String appointmentLabel
    ) {
        if (customerPhone == null || customerPhone.isBlank()) {
            return;
        }

        Map<String, String> variables = buildVariables(
                customerName, serviceName, staffName, startsAt, clinicName, appointmentLabel
        );

        OffsetDateTime now = OffsetDateTime.now();

        // 1. Immediate Booking Confirmation
        MessageTemplateDto confTemplate = templateRepository.find(tenantId, "confirmation", "whatsapp", "en");
        if (confTemplate.isActive()) {
            String body = templateEngine.render(confTemplate.body(), variables);
            String dedupe = "appt:" + appointmentId + ":confirmation";
            notificationRepository.enqueue(
                    tenantId,
                    "appointment",
                    appointmentId,
                    customerPhone,
                    customerName,
                    "confirmation",
                    "whatsapp",
                    "mock",
                    body,
                    now,
                    dedupe
            );
            log.info("Enqueued booking confirmation for appointment #{}", appointmentId);
        }

        // 2. 24-Hour Reminder
        OffsetDateTime reminder24h = startsAt.minusHours(24);
        if (reminder24h.isAfter(now)) {
            MessageTemplateDto rem24Template = templateRepository.find(tenantId, "reminder_24h", "whatsapp", "en");
            if (rem24Template.isActive()) {
                String body = templateEngine.render(rem24Template.body(), variables);
                String dedupe = "appt:" + appointmentId + ":reminder_24h:" + startsAt.toEpochSecond();
                notificationRepository.enqueue(
                        tenantId,
                        "appointment",
                        appointmentId,
                        customerPhone,
                        customerName,
                        "reminder_24h",
                        "whatsapp",
                        "mock",
                        body,
                        reminder24h,
                        dedupe
                );
                log.info("Enqueued 24h reminder for appointment #{} at {}", appointmentId, reminder24h);
            }
        }

        // 3. 2-Hour Reminder
        OffsetDateTime reminder2h = startsAt.minusHours(2);
        if (reminder2h.isAfter(now)) {
            MessageTemplateDto rem2Template = templateRepository.find(tenantId, "reminder_2h", "whatsapp", "en");
            if (rem2Template.isActive()) {
                String body = templateEngine.render(rem2Template.body(), variables);
                String dedupe = "appt:" + appointmentId + ":reminder_2h:" + startsAt.toEpochSecond();
                notificationRepository.enqueue(
                        tenantId,
                        "appointment",
                        appointmentId,
                        customerPhone,
                        customerName,
                        "reminder_2h",
                        "whatsapp",
                        "mock",
                        body,
                        reminder2h,
                        dedupe
                );
                log.info("Enqueued 2h reminder for appointment #{} at {}", appointmentId, reminder2h);
            }
        }
    }

    @Override
    @Transactional
    public void handleAppointmentRescheduled(
            Long tenantId,
            Long appointmentId,
            String customerPhone,
            String customerName,
            String serviceName,
            String staffName,
            OffsetDateTime newStartsAt,
            String clinicName,
            String appointmentLabel
    ) {
        log.info("Cancelling outdated queued reminders for rescheduled appointment #{}", appointmentId);
        notificationRepository.cancelByRef(tenantId, "appointment", appointmentId);

        Map<String, String> variables = buildVariables(
                customerName, serviceName, staffName, newStartsAt, clinicName, appointmentLabel
        );

        OffsetDateTime now = OffsetDateTime.now();

        // Queue updated 24h reminder if in the future
        OffsetDateTime reminder24h = newStartsAt.minusHours(24);
        if (reminder24h.isAfter(now)) {
            MessageTemplateDto rem24Template = templateRepository.find(tenantId, "reminder_24h", "whatsapp", "en");
            if (rem24Template.isActive()) {
                String body = templateEngine.render(rem24Template.body(), variables);
                String dedupe = "appt:" + appointmentId + ":reminder_24h:" + newStartsAt.toEpochSecond();
                notificationRepository.enqueue(
                        tenantId,
                        "appointment",
                        appointmentId,
                        customerPhone,
                        customerName,
                        "reminder_24h",
                        "whatsapp",
                        "mock",
                        body,
                        reminder24h,
                        dedupe
                );
            }
        }

        // Queue updated 2h reminder if in the future
        OffsetDateTime reminder2h = newStartsAt.minusHours(2);
        if (reminder2h.isAfter(now)) {
            MessageTemplateDto rem2Template = templateRepository.find(tenantId, "reminder_2h", "whatsapp", "en");
            if (rem2Template.isActive()) {
                String body = templateEngine.render(rem2Template.body(), variables);
                String dedupe = "appt:" + appointmentId + ":reminder_2h:" + newStartsAt.toEpochSecond();
                notificationRepository.enqueue(
                        tenantId,
                        "appointment",
                        appointmentId,
                        customerPhone,
                        customerName,
                        "reminder_2h",
                        "whatsapp",
                        "mock",
                        body,
                        reminder2h,
                        dedupe
                );
            }
        }
    }

    @Override
    @Transactional
    public void handleAppointmentCancelled(Long tenantId, Long appointmentId) {
        log.info("Cancelling all queued notifications for cancelled appointment #{}", appointmentId);
        notificationRepository.cancelByRef(tenantId, "appointment", appointmentId);
    }

    private Map<String, String> buildVariables(
            String customerName,
            String serviceName,
            String staffName,
            OffsetDateTime startsAt,
            String clinicName,
            String appointmentLabel
    ) {
        Map<String, String> vars = new HashMap<>();
        vars.put("customer_name", customerName != null ? customerName : "Valued Customer");
        vars.put("service_name", serviceName != null ? serviceName : "Service");
        vars.put("staff_name", staffName != null ? staffName : "Specialist");
        vars.put("date", startsAt.format(DATE_FORMATTER));
        vars.put("start_time", startsAt.format(TIME_FORMATTER));
        vars.put("clinic_name", clinicName != null ? clinicName : "Our Practice");
        vars.put("appointment_label", appointmentLabel != null ? appointmentLabel : "Appointment");
        return vars;
    }
}
