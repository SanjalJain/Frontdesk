package com.booking.core.internal.outbox;

import com.booking.core.api.event.DomainEvent;
import com.booking.core.api.event.OutboxEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxDispatcher implements OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxDispatcher(
            OutboxRepository outboxRepository,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enqueue(DomainEvent event) {
        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            outboxRepository.enqueue(
                    event.tenantId(),
                    event.eventType(),
                    event.aggregateType(),
                    event.aggregateId(),
                    payloadJson
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize domain event: " + event, e);
        }
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void dispatchPendingEvents() {
        List<OutboxRepository.OutboxRecord> records = outboxRepository.fetchUnpublished(20);
        for (var record : records) {
            try {
                log.info("Dispatching outbox event id={}, type={}, aggregate={}:{}",
                        record.id(), record.eventType(), record.aggregateType(), record.aggregateId());
                eventPublisher.publishEvent(record);
                outboxRepository.markPublished(record.id());
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}: {}", record.id(), e.getMessage(), e);
            }
        }
    }
}
