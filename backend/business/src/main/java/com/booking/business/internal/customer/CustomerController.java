package com.booking.business.internal.customer;

import com.booking.business.api.dto.CreateCustomerRequest;
import com.booking.business.api.dto.CustomerDto;
import com.booking.core.api.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/biz/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<?> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        try {
            CustomerDto created = customerService.createCustomer(tenantId, request, "IN");
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (DuplicateCustomerPhoneException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "DUPLICATE_PHONE",
                    "message", e.getMessage(),
                    "existingCustomerId", e.getExistingCustomerId(),
                    "phone", e.getPhone()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_PHONE",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<List<CustomerDto>> searchCustomers(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(customerService.searchCustomers(tenantId, q, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable(name = "id") Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return customerService.findById(tenantId, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
