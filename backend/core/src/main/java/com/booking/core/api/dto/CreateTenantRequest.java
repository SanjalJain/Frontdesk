package com.booking.core.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateTenantRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9-]{3,40}$", message = "Slug must be 3-40 lowercase letters, numbers, or hyphens")
        String slug,

        @NotBlank
        String name,

        @NotBlank
        String vertical,

        String timezone,
        String currency,
        Integer slotMinutes,

        @NotBlank
        @Email
        String ownerEmail,

        @NotBlank
        String ownerPassword,

        @NotBlank
        String ownerName
) {}
