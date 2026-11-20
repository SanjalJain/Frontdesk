package com.booking.business.internal.customer;

import com.booking.business.api.dto.CreateCustomerRequest;
import com.booking.business.api.dto.CustomerDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PhoneNormalizationService phoneNormalizationService;

    public CustomerService(
            CustomerRepository customerRepository,
            PhoneNormalizationService phoneNormalizationService
    ) {
        this.customerRepository = customerRepository;
        this.phoneNormalizationService = phoneNormalizationService;
    }

    @Transactional
    public CustomerDto createCustomer(Long tenantId, CreateCustomerRequest request, String defaultCountry) {
        String normalizedPhone = phoneNormalizationService.normalizeToE164(request.phone(), defaultCountry);

        Optional<CustomerDto> existing = customerRepository.findByPhone(tenantId, normalizedPhone);
        if (existing.isPresent()) {
            throw new DuplicateCustomerPhoneException(
                    "Customer with phone number " + normalizedPhone + " already exists in this practice.",
                    existing.get().id(),
                    normalizedPhone
            );
        }

        return customerRepository.create(
                tenantId,
                request.fullName(),
                normalizedPhone,
                request.email(),
                request.dob(),
                request.notes(),
                request.intakeJson()
        );
    }

    public Optional<CustomerDto> findById(Long tenantId, Long id) {
        return customerRepository.findById(tenantId, id);
    }

    public List<CustomerDto> searchCustomers(Long tenantId, String query, int limit) {
        return customerRepository.search(tenantId, query, limit > 0 ? limit : 20);
    }
}
