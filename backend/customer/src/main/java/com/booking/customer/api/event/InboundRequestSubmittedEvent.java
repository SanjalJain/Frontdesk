package com.booking.customer.api.event;

import java.time.Instant;

public record InboundRequestSubmittedEvent(
        Long tenantId,
        Long requestId,
        String type,
        String customerName,
        String phoneE164,
        Instant submittedAt
) {}
