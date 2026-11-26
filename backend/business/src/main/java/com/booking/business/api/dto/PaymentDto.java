package com.booking.business.api.dto;

import java.time.OffsetDateTime;

public record PaymentDto(
        Long id,
        Long tenantId,
        Long customerId,
        String customerName,
        String customerPhone,
        Long appointmentId,
        Integer amountPaise,
        String method,
        String reference,
        String receiptNo,
        Long receivedBy,
        String receivedByName,
        OffsetDateTime receivedAt,
        String note
) {}
