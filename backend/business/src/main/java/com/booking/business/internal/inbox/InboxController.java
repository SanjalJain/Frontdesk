package com.booking.business.internal.inbox;

import com.booking.core.api.TenantContext;
import com.booking.customer.api.InboundRequestBridgeService;
import com.booking.customer.api.dto.HandleInboundRequest;
import com.booking.customer.api.dto.InboundRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/biz/inbox")
public class InboxController {

    private final InboundRequestBridgeService inboundRequestBridgeService;

    public InboxController(InboundRequestBridgeService inboundRequestBridgeService) {
        this.inboundRequestBridgeService = inboundRequestBridgeService;
    }

    @GetMapping
    public ResponseEntity<List<InboundRequestDto>> listPendingRequests() {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(inboundRequestBridgeService.listPendingRequests(tenantId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> handleRequest(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody HandleInboundRequest request
    ) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.getUserId();
        boolean handled = inboundRequestBridgeService.handleRequest(tenantId, id, request, userId);
        if (handled) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
