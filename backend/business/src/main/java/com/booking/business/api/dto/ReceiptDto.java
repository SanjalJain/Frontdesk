package com.booking.business.api.dto;

import java.time.OffsetDateTime;

public record ReceiptDto(
        String receiptNo,
        Long tenantId,
        String clinicName,
        String clinicAddress,
        String clinicPhone,
        Long customerId,
        String customerName,
        String customerPhone,
        Long appointmentId,
        String serviceName,
        Integer amountPaise,
        String method,
        String reference,
        String receivedByName,
        OffsetDateTime receivedAt,
        String note
) {}
