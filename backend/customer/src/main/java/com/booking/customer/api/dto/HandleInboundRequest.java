package com.booking.customer.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record HandleInboundRequest(
        @NotBlank(message = "status is required")
        @Pattern(regexp = "accepted|declined", message = "status must be accepted or declined")
        String status,

        Long convertedAppointmentId
) {}
