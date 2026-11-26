package com.booking.core.api.notification;

import jakarta.validation.constraints.NotBlank;

public record UpdateTemplateRequest(
        @NotBlank(message = "Template body is required")
        String body,

        Boolean isActive
) {}
