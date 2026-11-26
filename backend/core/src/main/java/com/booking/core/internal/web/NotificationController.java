package com.booking.core.internal.web;

import com.booking.core.api.TenantContext;
import com.booking.core.api.notification.NotificationDto;
import com.booking.core.internal.notification.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/biz/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/recent")
    public ResponseEntity<List<NotificationDto>> listRecent(
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(notificationRepository.listRecent(tenantId, limit));
    }

    @GetMapping("/failed")
    public ResponseEntity<List<NotificationDto>> listFailed(
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(notificationRepository.listFailed(tenantId, limit));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retryNotification(
            @PathVariable(name = "id") Long id
    ) {
        Long tenantId = TenantContext.requireTenantId();
        notificationRepository.retry(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
