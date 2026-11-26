package com.booking.business.api.event;

import com.booking.core.api.event.DomainEvent;

import java.time.Instant;

public record AppointmentRescheduledEvent(
        Long tenantId,
        Long appointmentId,
        Long customerId,
        Long serviceId,
        Long staffId,
        Instant oldStartsAt,
        Instant newStartsAt,
        Instant newEndsAt,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String eventType() {
        return "AppointmentRescheduled";
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
