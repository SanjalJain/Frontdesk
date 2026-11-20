package com.booking.core.api.event;

import java.time.Instant;

/**
 * Common contract for all domain events published via the Transactional Outbox.
 */
public interface DomainEvent {
    Long tenantId();
    String eventType();
    String aggregateType();
    String aggregateId();
    Instant occurredAt();
}
