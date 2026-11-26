package com.booking.business.internal.scheduling;

import com.booking.business.api.dto.CreateWaitlistRequest;
import com.booking.business.api.dto.WaitlistEntryDto;
import com.booking.core.api.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/biz/waitlist")
public class WaitlistController {

    private final WaitlistRepository waitlistRepository;

    public WaitlistController(WaitlistRepository waitlistRepository) {
        this.waitlistRepository = waitlistRepository;
    }

    @GetMapping
    public ResponseEntity<List<WaitlistEntryDto>> listActive() {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(waitlistRepository.listActive(tenantId));
    }

    @PostMapping
    public ResponseEntity<WaitlistEntryDto> create(@Valid @RequestBody CreateWaitlistRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        WaitlistEntryDto created = waitlistRepository.create(
                tenantId,
                request.customerId(),
                request.serviceId(),
                request.staffId(),
                request.earliestDate(),
                request.latestDate(),
                request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable(name = "id") Long id,
            @RequestBody Map<String, String> body
    ) {
        Long tenantId = TenantContext.requireTenantId();
        String status = body.getOrDefault("status", "removed");
        waitlistRepository.updateStatus(tenantId, id, status);
        return ResponseEntity.noContent().build();
    }
}
