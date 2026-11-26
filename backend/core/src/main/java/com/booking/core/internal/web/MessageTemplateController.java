package com.booking.core.internal.web;

import com.booking.core.api.TenantContext;
import com.booking.core.api.notification.MessageTemplateDto;
import com.booking.core.api.notification.UpdateTemplateRequest;
import com.booking.core.internal.notification.MessageTemplateRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/biz/notifications/templates")
public class MessageTemplateController {

    private final MessageTemplateRepository templateRepository;

    public MessageTemplateController(MessageTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @GetMapping
    public ResponseEntity<List<MessageTemplateDto>> listTemplates() {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(templateRepository.listByTenant(tenantId));
    }

    @PutMapping("/{kind}")
    public ResponseEntity<MessageTemplateDto> updateTemplate(
            @PathVariable(name = "kind") String kind,
            @Valid @RequestBody UpdateTemplateRequest request,
            @RequestParam(name = "channel", defaultValue = "whatsapp") String channel,
            @RequestParam(name = "language", defaultValue = "en") String language
    ) {
        Long tenantId = TenantContext.requireTenantId();
        boolean isActive = request.isActive() != null ? request.isActive() : true;

        MessageTemplateDto updated = templateRepository.upsert(
                tenantId,
                kind,
                channel,
                language,
                request.body(),
                isActive
        );
        return ResponseEntity.ok(updated);
    }
}
