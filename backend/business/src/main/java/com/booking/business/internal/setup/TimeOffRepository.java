package com.booking.business.internal.setup;

import com.booking.business.api.dto.TimeOffDto;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.jooq.impl.DSL.*;

@Repository
public class TimeOffRepository {

    private final DSLContext dsl;

    public TimeOffRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public TimeOffDto create(
            Long tenantId,
            Long staffId,
            Instant startsAt,
            Instant endsAt,
            String reason
    ) {
        var record = dsl.insertInto(table(name("business", "time_off")))
                .columns(
                        field(name("tenant_id"), Long.class),
                        field(name("staff_id"), Long.class),
                        field(name("starts_at"), OffsetDateTime.class),
                        field(name("ends_at"), OffsetDateTime.class),
                        field(name("reason"), String.class)
                )
                .values(
                        tenantId,
                        staffId,
                        startsAt.atOffset(OffsetDateTime.now().getOffset()),
                        endsAt.atOffset(OffsetDateTime.now().getOffset()),
                        reason
                )
                .returning(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("staff_id"), Long.class),
                        field(name("starts_at"), OffsetDateTime.class),
                        field(name("ends_at"), OffsetDateTime.class),
                        field(name("reason"), String.class)
                )
                .fetchOne();

        return mapToDto(record);
    }

    public List<TimeOffDto> listByTenant(Long tenantId) {
        return dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("staff_id"), Long.class),
                        field(name("starts_at"), OffsetDateTime.class),
                        field(name("ends_at"), OffsetDateTime.class),
                        field(name("reason"), String.class)
                )
                .from(table(name("business", "time_off")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .orderBy(field(name("starts_at"), OffsetDateTime.class).asc())
                .fetch(this::mapToDto);
    }

    public void delete(Long tenantId, Long id) {
        dsl.deleteFrom(table(name("business", "time_off")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("id"), Long.class).eq(id)
                )
                .execute();
    }

    private TimeOffDto mapToDto(Record r) {
        if (r == null) return null;
        OffsetDateTime starts = r.get(field(name("starts_at"), OffsetDateTime.class));
        OffsetDateTime ends = r.get(field(name("ends_at"), OffsetDateTime.class));
        return new TimeOffDto(
                r.get(field(name("id"), Long.class)),
                r.get(field(name("tenant_id"), Long.class)),
                r.get(field(name("staff_id"), Long.class)),
                starts != null ? starts.toInstant() : Instant.now(),
                ends != null ? ends.toInstant() : Instant.now(),
                r.get(field(name("reason"), String.class))
        );
    }
}
