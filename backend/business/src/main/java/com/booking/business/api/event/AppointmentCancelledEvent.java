package com.booking.business.api.event;

import com.booking.core.api.event.DomainEvent;

import java.time.Instant;

public record AppointmentCancelledEvent(
        Long tenantId,
        Long appointmentId,
        Long customerId,
        Long staffId,
        String reason,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String eventType() {
        return "AppointmentCancelled";
    }

    @Override
    public String aggregateType() {
        return "Appointment";
    }

    @Override
    public String aggregateId() {
        return String.valueOf(appointmentId);
    }
}
