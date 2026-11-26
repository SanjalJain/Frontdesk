package com.booking.business.internal.scheduling;

import com.booking.business.api.dto.*;
import com.booking.core.api.TenantContext;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/biz/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<AppointmentDto>> listAppointments(
            @RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(name = "staffId", required = false) Long staffId
    ) {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(appointmentService.listAppointments(tenantId, from, to, staffId));
    }

    @GetMapping("/tracker")
    public ResponseEntity<List<TrackerAppointmentDto>> listTracker(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(name = "staffId", required = false) Long staffId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "limit", defaultValue = "200") int limit
    ) {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(appointmentService.listTracker(tenantId, from, to, staffId, status, search, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDto> getAppointment(@PathVariable(name = "id") Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return appointmentService.getAppointment(tenantId, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AppointmentDto> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        AppointmentDto created = appointmentService.bookAppointment(tenantId, request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentDto> rescheduleAppointment(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody RescheduleAppointmentRequest request
    ) {
        Long tenantId = TenantContext.requireTenantId();
        AppointmentDto rescheduled = appointmentService.rescheduleAppointment(tenantId, id, request);
        return ResponseEntity.ok(rescheduled);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentDto> updateStatus(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody UpdateAppointmentStatusRequest request
    ) {
        Long tenantId = TenantContext.requireTenantId();
        AppointmentDto updated = appointmentService.updateStatus(tenantId, id, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/daysheet")
    public ResponseEntity<DaySheetDto> getDaySheet(
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(appointmentService.getDaySheet(tenantId, date));
    }
}
