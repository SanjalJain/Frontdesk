package com.booking.core.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Practice name is required")
        String practiceName,

        @NotBlank(message = "Practice URL slug is required")
        @Pattern(regexp = "^[a-z0-9-]{3,40}$", message = "URL slug must be 3 to 40 lowercase letters, numbers, or hyphens")
        String slug,

        @NotBlank(message = "Practice type is required (dental, salon, generic)")
        String vertical,

        @NotBlank(message = "Owner full name is required")
        String ownerName,

        @NotBlank(message = "Email address is required")
        @Email(message = "Please enter a valid email address (e.g. doctor@clinic.com)")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters long")
        String password
) {}
