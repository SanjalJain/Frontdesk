package com.booking.customer.internal.web;

import com.booking.customer.api.dto.InboundRequestDto;
import com.booking.customer.api.dto.PublicTenantProfileDto;
import com.booking.customer.api.dto.SubmitInboundRequest;
import com.booking.customer.internal.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/{slug}")
public class PublicController {

    private final CustomerService customerService;

    public PublicController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/profile")
    public ResponseEntity<PublicTenantProfileDto> getPublicProfile(@PathVariable(name = "slug") String slug) {
        return customerService.getPublicProfile(slug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/requests")
    public ResponseEntity<InboundRequestDto> submitRequest(
            @PathVariable(name = "slug") String slug,
            @Valid @RequestBody SubmitInboundRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        InboundRequestDto created = customerService.submitRequest(slug, request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private String extractClientIp(HttpServletRequest req) {
        String xForwarded = req.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
