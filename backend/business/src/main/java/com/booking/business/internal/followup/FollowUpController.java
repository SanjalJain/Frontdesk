package com.booking.business.internal.followup;

import com.booking.business.api.dto.FollowUpCallDto;
import com.booking.business.api.dto.UpdateFollowUpRequest;
import com.booking.core.api.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/biz/follow-ups")
public class FollowUpController {

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    @GetMapping
    public ResponseEntity<List<FollowUpCallDto>> listFollowUps() {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(followUpService.listFollowUps(tenantId));
    }

    @PatchMapping("/{appointmentId}")
    public ResponseEntity<Void> updateFollowUp(
            @PathVariable(name = "appointmentId") Long appointmentId,
            @Valid @RequestBody UpdateFollowUpRequest request
    ) {
        Long tenantId = TenantContext.requireTenantId();
        boolean updated = followUpService.updateFollowUp(tenantId, appointmentId, request);
        if (updated) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
