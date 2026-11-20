package com.booking.core.internal.outbox;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.jooq.impl.DSL.*;

@Repository
public class OutboxRepository {

    private final DSLContext dsl;

    public OutboxRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void enqueue(
            Long tenantId,
            String eventType,
            String aggregateType,
            String aggregateId,
            String payloadJson
    ) {
        dsl.insertInto(table(name("core", "outbox_events")))
                .columns(
                        field(name("tenant_id"), Long.class),
                        field(name("event_type"), String.class),
                        field(name("aggregate_type"), String.class),
                        field(name("aggregate_id"), String.class),
                        field(name("payload"), JSONB.class)
                )
                .values(
                        tenantId,
                        eventType,
                        aggregateType,
                        aggregateId,
                        JSONB.valueOf(payloadJson != null ? payloadJson : "{}")
                )
                .execute();
    }

    public List<OutboxRecord> fetchUnpublished(int limit) {
        return dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("event_type"), String.class),
                        field(name("aggregate_type"), String.class),
                        field(name("aggregate_id"), String.class),
                        field(name("payload"), JSONB.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .from(table(name("core", "outbox_events")))
                .where(field(name("published_at")).isNull())
                .orderBy(field(name("created_at")).asc())
                .limit(limit)
                .forUpdate()
                .skipLocked()
                .fetch(this::mapToRecord);
    }

    public void markPublished(Long id) {
        dsl.update(table(name("core", "outbox_events")))
                .set(field(name("published_at")), currentOffsetDateTime())
                .where(field(name("id"), Long.class).eq(id))
                .execute();
    }

    private OutboxRecord mapToRecord(Record r) {
        OffsetDateTime created = r.get(field(name("created_at"), OffsetDateTime.class));
        return new OutboxRecord(
                r.get(field(name("id"), Long.class)),
                r.get(field(name("tenant_id"), Long.class)),
                r.get(field(name("event_type"), String.class)),
                r.get(field(name("aggregate_type"), String.class)),
                r.get(field(name("aggregate_id"), String.class)),
                r.get(field(name("payload"), JSONB.class)).data(),
                created != null ? created.toInstant() : Instant.now()
        );
    }

    public record OutboxRecord(
            Long id,
            Long tenantId,
            String eventType,
            String aggregateType,
            String aggregateId,
            String payload,
            Instant createdAt
    ) {}
}
