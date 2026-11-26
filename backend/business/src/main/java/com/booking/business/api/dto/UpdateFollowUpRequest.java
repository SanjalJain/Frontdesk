package com.booking.business.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record UpdateFollowUpRequest(
        @NotBlank(message = "status is required")
        @Pattern(regexp = "none|needed|called|booked|declined", message = "Invalid followup status")
        String status,

        LocalDate followupDueOn,

        String note
) {}
