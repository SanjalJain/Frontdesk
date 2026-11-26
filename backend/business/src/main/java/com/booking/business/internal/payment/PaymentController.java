package com.booking.business.internal.payment;

import com.booking.business.api.dto.CustomerBalanceDto;
import com.booking.business.api.dto.PaymentDto;
import com.booking.business.api.dto.ReceiptDto;
import com.booking.business.api.dto.RecordPaymentRequest;
import com.booking.core.api.TenantContext;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/biz/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentDto> recordPayment(@Valid @RequestBody RecordPaymentRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.getUserId();
        PaymentDto created = paymentService.recordPayment(tenantId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<PaymentDto>> listPayments(
            @RequestParam(name = "customerId", required = false) Long customerId,
            @RequestParam(name = "appointmentId", required = false) Long appointmentId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        Long tenantId = TenantContext.requireTenantId();
        List<PaymentDto> payments = paymentService.listPayments(tenantId, customerId, appointmentId, from, to, limit);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable(name = "id") Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return paymentService.getPayment(tenantId, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<ReceiptDto> getReceipt(@PathVariable(name = "id") Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return paymentService.getReceipt(tenantId, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/balance/{customerId}")
    public ResponseEntity<CustomerBalanceDto> getCustomerBalance(@PathVariable(name = "customerId") Long customerId) {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(paymentService.getCustomerBalance(tenantId, customerId));
    }
}
