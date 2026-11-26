package com.booking.core.internal.notification;

import com.booking.core.api.notification.NotificationDto;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
public class NotificationRepository {

    private final DSLContext dsl;

    public NotificationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public boolean enqueue(
            Long tenantId,
            String refType,
            Long refId,
            String customerPhone,
            String customerName,
            String kind,
            String channel,
            String provider,
            String body,
            OffsetDateTime scheduledFor,
            String dedupeKey
    ) {
        int inserted = dsl.insertInto(table(name("core", "notifications")))
                .columns(
                        field(name("tenant_id"), Long.class),
                        field(name("ref_type"), String.class),
                        field(name("ref_id"), Long.class),
                        field(name("customer_phone"), String.class),
                        field(name("customer_name"), String.class),
                        field(name("kind"), String.class),
                        field(name("channel"), String.class),
                        field(name("provider"), String.class),
                        field(name("body"), String.class),
                        field(name("scheduled_for"), OffsetDateTime.class),
                        field(name("status"), String.class),
                        field(name("dedupe_key"), String.class)
                )
                .values(
                        tenantId,
                        refType != null ? refType : "appointment",
                        refId,
                        customerPhone,
                        customerName,
                        kind,
                        channel != null ? channel : "whatsapp",
                        provider != null ? provider : "mock",
                        body,
                        scheduledFor,
                        "queued",
                        dedupeKey
                )
                .onConflict(
                        field(name("tenant_id"), Long.class),
                        field(name("dedupe_key"), String.class)
                )
                .doNothing()
                .execute();

        return inserted > 0;
    }

    public List<NotificationDto> fetchDueNotifications(int limit) {
        return dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("ref_type"), String.class),
                        field(name("ref_id"), Long.class),
                        field(name("customer_phone"), String.class),
                        field(name("customer_name"), String.class),
                        field(name("kind"), String.class),
                        field(name("channel"), String.class),
                        field(name("provider"), String.class),
                        field(name("body"), String.class),
                        field(name("scheduled_for"), OffsetDateTime.class),
                        field(name("status"), String.class),
                        field(name("attempts"), Short.class),
                        field(name("next_attempt_at"), OffsetDateTime.class),
                        field(name("last_error"), String.class),
                        field(name("provider_message_id"), String.class),
                        field(name("sent_at"), OffsetDateTime.class),
                        field(name("dedupe_key"), String.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .from(table(name("core", "notifications")))
                .where(
                        field(name("status"), String.class).eq("queued"),
                        field(name("scheduled_for"), OffsetDateTime.class).le(currentOffsetDateTime())
                )
                .orderBy(field(name("scheduled_for"), OffsetDateTime.class).asc())
                .limit(limit)
                .forUpdate()
                .skipLocked()
                .fetch(this::mapToDto);
    }

    public void markSent(Long id, String providerMessageId) {
        dsl.update(table(name("core", "notifications")))
                .set(field(name("status"), String.class), "sent")
                .set(field(name("sent_at"), OffsetDateTime.class), currentOffsetDateTime())
                .set(field(name("provider_message_id"), String.class), providerMessageId)
                .where(field(name("id"), Long.class).eq(id))
                .execute();
    }

    public void markFailed(Long id, String error, int attempts, OffsetDateTime nextAttemptAt) {
        String status = attempts >= 3 ? "failed" : "queued";
        dsl.update(table(name("core", "notifications")))
                .set(field(name("status"), String.class), status)
                .set(field(name("attempts"), Short.class), (short) attempts)
                .set(field(name("last_error"), String.class), error)
                .set(field(name("next_attempt_at"), OffsetDateTime.class), nextAttemptAt)
                .where(field(name("id"), Long.class).eq(id))
                .execute();
    }

    public void cancelByRef(Long tenantId, String refType, Long refId) {
        dsl.update(table(name("core", "notifications")))
                .set(field(name("status"), String.class), "cancelled")
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("ref_type"), String.class).eq(refType),
                        field(name("ref_id"), Long.class).eq(refId),
                        field(name("status"), String.class).eq("queued")
                )
                .execute();
    }

    public List<NotificationDto> listRecent(Long tenantId, int limit) {
        return dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("ref_type"), String.class),
                        field(name("ref_id"), Long.class),
                        field(name("customer_phone"), String.class),
                        field(name("customer_name"), String.class),
                        field(name("kind"), String.class),
                        field(name("channel"), String.class),
                        field(name("provider"), String.class),
                        field(name("body"), String.class),
                        field(name("scheduled_for"), OffsetDateTime.class),
                        field(name("status"), String.class),
                        field(name("attempts"), Short.class),
                        field(name("next_attempt_at"), OffsetDateTime.class),
                        field(name("last_error"), String.class),
                        field(name("provider_message_id"), String.class),
                        field(name("sent_at"), OffsetDateTime.class),
                        field(name("dedupe_key"), String.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .from(table(name("core", "notifications")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .orderBy(field(name("created_at"), OffsetDateTime.class).desc())
                .limit(limit)
                .fetch(this::mapToDto);
    }

    public List<NotificationDto> listFailed(Long tenantId, int limit) {
        return dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("ref_type"), String.class),
                        field(name("ref_id"), Long.class),
                        field(name("customer_phone"), String.class),
                        field(name("customer_name"), String.class),
                        field(name("kind"), String.class),
                        field(name("channel"), String.class),
                        field(name("provider"), String.class),
                        field(name("body"), String.class),
                        field(name("scheduled_for"), OffsetDateTime.class),
                        field(name("status"), String.class),
                        field(name("attempts"), Short.class),
                        field(name("next_attempt_at"), OffsetDateTime.class),
                        field(name("last_error"), String.class),
                        field(name("provider_message_id"), String.class),
                        field(name("sent_at"), OffsetDateTime.class),
                        field(name("dedupe_key"), String.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .from(table(name("core", "notifications")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("status"), String.class).eq("failed")
                )
                .orderBy(field(name("created_at"), OffsetDateTime.class).desc())
                .limit(limit)
                .fetch(this::mapToDto);
    }

    public Optional<NotificationDto> findById(Long tenantId, Long id) {
        var record = dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("ref_type"), String.class),
                        field(name("ref_id"), Long.class),
                        field(name("customer_phone"), String.class),
                        field(name("customer_name"), String.class),
                        field(name("kind"), String.class),
                        field(name("channel"), String.class),
                        field(name("provider"), String.class),
                        field(name("body"), String.class),
                        field(name("scheduled_for"), OffsetDateTime.class),
                        field(name("status"), String.class),
                        field(name("attempts"), Short.class),
                        field(name("next_attempt_at"), OffsetDateTime.class),
                        field(name("last_error"), String.class),
                        field(name("provider_message_id"), String.class),
                        field(name("sent_at"), OffsetDateTime.class),
                        field(name("dedupe_key"), String.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .from(table(name("core", "notifications")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("id"), Long.class).eq(id)
                )
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToDto);
    }

    public void retry(Long tenantId, Long id) {
        dsl.update(table(name("core", "notifications")))
                .set(field(name("status"), String.class), "queued")
                .set(field(name("attempts"), Short.class), (short) 0)
                .set(field(name("scheduled_for"), OffsetDateTime.class), currentOffsetDateTime())
                .set(field(name("last_error"), String.class), (String) null)
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("id"), Long.class).eq(id)
                )
                .execute();
    }

    private NotificationDto mapToDto(Record r) {
        if (r == null) return null;
        OffsetDateTime created = r.get(field(name("created_at"), OffsetDateTime.class));
        Short attempts = r.get(field(name("attempts"), Short.class));

        return new NotificationDto(
                r.get(field(name("id"), Long.class)),
                r.get(field(name("tenant_id"), Long.class)),
                r.get(field(name("ref_type"), String.class)),
                r.get(field(name("ref_id"), Long.class)),
                r.get(field(name("customer_phone"), String.class)),
                r.get(field(name("customer_name"), String.class)),
                r.get(field(name("kind"), String.class)),
                r.get(field(name("channel"), String.class)),
                r.get(field(name("provider"), String.class)),
                r.get(field(name("body"), String.class)),
                r.get(field(name("scheduled_for"), OffsetDateTime.class)),
                r.get(field(name("status"), String.class)),
                attempts != null ? attempts.intValue() : 0,
                r.get(field(name("next_attempt_at"), OffsetDateTime.class)),
                r.get(field(name("last_error"), String.class)),
                r.get(field(name("provider_message_id"), String.class)),
                r.get(field(name("sent_at"), OffsetDateTime.class)),
                r.get(field(name("dedupe_key"), String.class)),
                created != null ? created.toInstant() : Instant.now()
        );
    }
}
