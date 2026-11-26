package com.booking.business.api.event;

import com.booking.core.api.event.DomainEvent;

import java.time.Instant;

public record AppointmentBookedEvent(
        Long tenantId,
        Long appointmentId,
        Long customerId,
        Long serviceId,
        Long staffId,
        Instant startsAt,
        Instant endsAt,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String eventType() {
        return "AppointmentBooked";
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
