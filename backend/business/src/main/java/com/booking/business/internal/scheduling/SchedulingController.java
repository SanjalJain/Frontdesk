package com.booking.business.internal.scheduling;

import com.booking.business.api.dto.AvailableSlotDto;
import com.booking.core.api.TenantContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/biz/scheduling")
public class SchedulingController {

    private final AppointmentService appointmentService;

    public SchedulingController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/availability")
    public ResponseEntity<List<AvailableSlotDto>> getAvailability(
            @RequestParam(name = "serviceId") Long serviceId,
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "staffId", required = false) Long staffId
    ) {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(appointmentService.getAvailability(tenantId, serviceId, date, staffId));
    }
}
