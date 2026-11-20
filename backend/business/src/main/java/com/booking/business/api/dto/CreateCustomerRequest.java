package com.booking.business.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateCustomerRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Phone number is required")
        String phone,

        String email,
        LocalDate dob,
        String notes,
        String intakeJson
) {}
