package com.booking.core.api.event;

public interface OutboxEventPublisher {
    void enqueue(DomainEvent event);
}
