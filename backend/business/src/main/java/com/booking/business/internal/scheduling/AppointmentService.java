package com.booking.business.internal.scheduling;

import com.booking.business.api.dto.*;
import com.booking.business.api.event.AppointmentBookedEvent;
import com.booking.business.api.event.AppointmentCancelledEvent;
import com.booking.business.api.event.AppointmentRescheduledEvent;
import com.booking.business.api.event.AppointmentStatusChangedEvent;
import com.booking.business.internal.customer.CustomerRepository;
import com.booking.business.internal.setup.ServiceRepository;
import com.booking.business.internal.setup.StaffRepository;
import com.booking.business.internal.setup.TimeOffRepository;
import com.booking.business.internal.setup.WorkingHoursRepository;
import com.booking.core.api.TenantLookupService;
import com.booking.core.api.dto.TenantDto;
import com.booking.core.api.event.OutboxEventPublisher;
import com.booking.core.api.notification.NotificationQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final TimeOffRepository timeOffRepository;
    private final TenantLookupService tenantLookupService;
    private final AvailabilityEngine availabilityEngine;
    private final OutboxEventPublisher outboxEventPublisher;
    private final NotificationQueueService notificationQueueService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            ServiceRepository serviceRepository,
            StaffRepository staffRepository,
            CustomerRepository customerRepository,
            WorkingHoursRepository workingHoursRepository,
            TimeOffRepository timeOffRepository,
            TenantLookupService tenantLookupService,
            AvailabilityEngine availabilityEngine,
            OutboxEventPublisher outboxEventPublisher,
            NotificationQueueService notificationQueueService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
        this.staffRepository = staffRepository;
        this.customerRepository = customerRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.timeOffRepository = timeOffRepository;
        this.tenantLookupService = tenantLookupService;
        this.availabilityEngine = availabilityEngine;
        this.outboxEventPublisher = outboxEventPublisher;
        this.notificationQueueService = notificationQueueService;
    }

    @Transactional
    public AppointmentDto bookAppointment(Long tenantId, CreateAppointmentRequest request, Long createdBy) {
        ServiceDto service = serviceRepository.findById(tenantId, request.serviceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + request.serviceId()));

        staffRepository.findById(tenantId, request.staffId())
                .orElseThrow(() -> new IllegalArgumentException("Staff not found with id: " + request.staffId()));

        customerRepository.findById(tenantId, request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + request.customerId()));

        OffsetDateTime startsAt = request.startsAt();
        OffsetDateTime endsAt = startsAt.plusMinutes(service.durationMin());
        int pricePaise = request.pricePaise() != null ? request.pricePaise() : service.pricePaise();

        try {
            AppointmentDto created = appointmentRepository.create(
                    tenantId,
                    request.customerId(),
                    request.serviceId(),
                    request.staffId(),
                    startsAt,
                    endsAt,
                    request.source(),
                    request.requestNote(),
                    pricePaise,
                    createdBy
            );

            outboxEventPublisher.enqueue(new AppointmentBookedEvent(
                    tenantId,
                    created.id(),
                    created.customerId(),
                    created.serviceId(),
                    created.staffId(),
                    created.startsAt().toInstant(),
                    created.endsAt().toInstant(),
                    Instant.now()
            ));

            // Queue reminders & confirmation
            String clinicName = tenantLookupService.findById(tenantId).map(TenantDto::name).orElse("Our Practice");
            String vertical = tenantLookupService.findById(tenantId).map(TenantDto::vertical).orElse("dental");
            String apptLabel = "salon".equalsIgnoreCase(vertical) ? "Booking" : "Appointment";

            notificationQueueService.scheduleAppointmentNotifications(
                    tenantId,
                    created.id(),
                    created.customerPhone(),
                    created.customerName(),
                    created.serviceName(),
                    created.staffName(),
                    created.startsAt(),
                    clinicName,
                    apptLabel
            );

            return created;
        } catch (Exception ex) {
            handleOverlapConflict(ex);
            throw ex;
        }
    }

    @Transactional
    public AppointmentDto rescheduleAppointment(Long tenantId, Long id, RescheduleAppointmentRequest request) {
        AppointmentDto current = appointmentRepository.findById(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));

        ServiceDto service = serviceRepository.findById(tenantId, current.serviceId())
                .orElseThrow(() -> new IllegalStateException("Service record missing for appointment"));

        Long targetStaffId = request.staffId() != null ? request.staffId() : current.staffId();
        OffsetDateTime newStartsAt = request.startsAt();
        OffsetDateTime newEndsAt = newStartsAt.plusMinutes(service.durationMin());

        try {
            AppointmentDto updated = appointmentRepository.reschedule(
                    tenantId,
                    id,
                    targetStaffId,
                    newStartsAt,
                    newEndsAt,
                    request.version()
            );

            outboxEventPublisher.enqueue(new AppointmentRescheduledEvent(
                    tenantId,
                    updated.id(),
                    updated.customerId(),
                    updated.serviceId(),
                    updated.staffId(),
                    current.startsAt().toInstant(),
                    updated.startsAt().toInstant(),
                    updated.endsAt().toInstant(),
                    Instant.now()
            ));

            // Reschedule reminders
            String clinicName = tenantLookupService.findById(tenantId).map(TenantDto::name).orElse("Our Practice");
            String vertical = tenantLookupService.findById(tenantId).map(TenantDto::vertical).orElse("dental");
            String apptLabel = "salon".equalsIgnoreCase(vertical) ? "Booking" : "Appointment";

            notificationQueueService.handleAppointmentRescheduled(
                    tenantId,
                    updated.id(),
                    updated.customerPhone(),
                    updated.customerName(),
                    updated.serviceName(),
                    updated.staffName(),
                    updated.startsAt(),
                    clinicName,
                    apptLabel
            );

            return updated;
        } catch (Exception ex) {
            handleOverlapConflict(ex);
            throw ex;
        }
    }

    @Transactional
    public AppointmentDto updateStatus(Long tenantId, Long id, UpdateAppointmentStatusRequest request) {
        AppointmentDto current = appointmentRepository.findById(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));

        AppointmentDto updated = appointmentRepository.updateStatus(
                tenantId,
                id,
                request.status(),
                request.note(),
                request.outcome(),
                request.reaction(),
                request.reactionNote(),
                request.cancelReason(),
                request.noShowReason(),
                request.version()
        );

        if ("cancelled".equalsIgnoreCase(request.status())) {
            notificationQueueService.handleAppointmentCancelled(tenantId, updated.id());

            outboxEventPublisher.enqueue(new AppointmentCancelledEvent(
                    tenantId,
                    updated.id(),
                    updated.customerId(),
                    updated.staffId(),
                    request.cancelReason() != null ? request.cancelReason() : request.note(),
                    Instant.now()
            ));
        } else {
            outboxEventPublisher.enqueue(new AppointmentStatusChangedEvent(
                    tenantId,
                    updated.id(),
                    updated.customerId(),
                    updated.staffId(),
                    current.status(),
                    updated.status(),
                    request.note(),
                    Instant.now()
            ));
        }

        return updated;
    }

    public List<AppointmentDto> listAppointments(Long tenantId, OffsetDateTime from, OffsetDateTime to, Long staffId) {
        return appointmentRepository.listByRange(tenantId, from, to, staffId);
    }

    public Optional<AppointmentDto> getAppointment(Long tenantId, Long id) {
        return appointmentRepository.findById(tenantId, id);
    }

    public List<TrackerAppointmentDto> listTracker(
            Long tenantId,
            OffsetDateTime from,
            OffsetDateTime to,
            Long staffId,
            String status,
            String search,
            int limit
    ) {
        return appointmentRepository.listTracker(tenantId, from, to, staffId, status, search, limit);
    }

    public DaySheetDto getDaySheet(Long tenantId, LocalDate date) {
        ZoneId zoneId = getTenantZone(tenantId);
        List<AppointmentDto> appts = appointmentRepository.listDaySheet(tenantId, date, zoneId);

        int total = appts.size();
        int completed = (int) appts.stream().filter(a -> "completed".equalsIgnoreCase(a.status())).count();
        int arrived = (int) appts.stream().filter(a -> "arrived".equalsIgnoreCase(a.status())).count();
        int inProgress = (int) appts.stream().filter(a -> "in_progress".equalsIgnoreCase(a.status())).count();
        int pendingOrBooked = (int) appts.stream().filter(a -> "booked".equalsIgnoreCase(a.status()) || "confirmed".equalsIgnoreCase(a.status())).count();

        Map<String, List<AppointmentDto>> byStaff = appts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.staffName() != null ? a.staffName() : "Unassigned",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return new DaySheetDto(
                date,
                total,
                completed,
                arrived,
                inProgress,
                pendingOrBooked,
                appts,
                byStaff
        );
    }

    public List<AvailableSlotDto> getAvailability(Long tenantId, Long serviceId, LocalDate date, Long requestedStaffId) {
        ServiceDto service = serviceRepository.findById(tenantId, serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        ZoneId zoneId = getTenantZone(tenantId);
        List<StaffDto> allStaff = staffRepository.listByTenant(tenantId);

        List<StaffDto> eligibleStaff;
        if (requestedStaffId != null) {
            eligibleStaff = allStaff.stream()
                    .filter(s -> s.id().equals(requestedStaffId))
                    .toList();
        } else {
            // Find staff assigned to this service
            eligibleStaff = allStaff.stream()
                    .filter(s -> s.serviceIds() != null && s.serviceIds().contains(serviceId))
                    .toList();
            if (eligibleStaff.isEmpty()) {
                // If no specific assignments, all active bookable staff are candidates
                eligibleStaff = allStaff.stream().filter(StaffDto::isBookable).toList();
            }
        }

        Map<Long, List<WorkingHoursDto>> workingHoursByStaff = new HashMap<>();
        for (StaffDto staff : eligibleStaff) {
            workingHoursByStaff.put(staff.id(), workingHoursRepository.listByStaff(tenantId, staff.id()));
        }

        List<TimeOffDto> timeOffList = timeOffRepository.listByTenant(tenantId);

        OffsetDateTime startOfDay = date.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
        List<AvailabilityEngine.AppointmentWindow> appointments = appointmentRepository.listActiveWindowsForDate(tenantId, startOfDay, endOfDay);

        int slotStepMinutes = 15;
        Optional<TenantDto> tenantOpt = tenantLookupService.findById(tenantId);
        if (tenantOpt.isPresent() && tenantOpt.get().slotMinutes() > 0) {
            slotStepMinutes = tenantOpt.get().slotMinutes();
        }

        return availabilityEngine.calculateAvailableSlots(
                service,
                date,
                zoneId,
                eligibleStaff,
                workingHoursByStaff,
                timeOffList,
                appointments,
                slotStepMinutes
        );
    }

    private ZoneId getTenantZone(Long tenantId) {
        return tenantLookupService.findById(tenantId)
                .map(t -> {
                    try {
                        return ZoneId.of(t.timezone());
                    } catch (Exception e) {
                        return ZoneId.of("Asia/Kolkata");
                    }
                })
                .orElse(ZoneId.of("Asia/Kolkata"));
    }

    private void handleOverlapConflict(Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
            if (root.getMessage() != null) {
                msg += " " + root.getMessage().toLowerCase();
            }
        }

        if (msg.contains("appointments_no_overlap") || msg.contains("exclusion constraint") || msg.contains("conflicting key")) {
            throw new DoubleBookingException("This staff member is already booked during this time window. Please choose another slot.");
        }
    }
}
