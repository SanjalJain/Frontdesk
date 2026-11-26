package com.booking.business.internal.scheduling;

import com.booking.business.api.dto.WaitlistEntryDto;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
public class WaitlistRepository {

    private final DSLContext dsl;

    public WaitlistRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public WaitlistEntryDto create(
            Long tenantId,
            Long customerId,
            Long serviceId,
            Long staffId,
            LocalDate earliestDate,
            LocalDate latestDate,
            String notes
    ) {
        var record = dsl.insertInto(table(name("business", "waitlist_entries")))
                .columns(
                        field(name("tenant_id"), Long.class),
                        field(name("customer_id"), Long.class),
                        field(name("service_id"), Long.class),
                        field(name("staff_id"), Long.class),
                        field(name("earliest_date"), LocalDate.class),
                        field(name("latest_date"), LocalDate.class),
                        field(name("notes"), String.class),
                        field(name("status"), String.class)
                )
                .values(
                        tenantId,
                        customerId,
                        serviceId,
                        staffId,
                        earliestDate,
                        latestDate,
                        notes,
                        "waiting"
                )
                .returning(field(name("id"), Long.class))
                .fetchOne();

        Long id = record.get(field(name("id"), Long.class));
        return findById(tenantId, id).orElseThrow(() -> new IllegalStateException("Failed to load waitlist entry"));
    }

    public Optional<WaitlistEntryDto> findById(Long tenantId, Long id) {
        var r = dsl.select(
                        field(name("w", "id"), Long.class),
                        field(name("w", "tenant_id"), Long.class),
                        field(name("w", "customer_id"), Long.class),
                        field(name("c", "full_name"), String.class).as("customer_name"),
                        field(name("c", "phone_e164"), String.class).as("customer_phone"),
                        field(name("w", "service_id"), Long.class),
                        field(name("s", "name"), String.class).as("service_name"),
                        field(name("w", "staff_id"), Long.class),
                        field(name("st", "name"), String.class).as("staff_name"),
                        field(name("w", "earliest_date"), LocalDate.class),
                        field(name("w", "latest_date"), LocalDate.class),
                        field(name("w", "notes"), String.class),
                        field(name("w", "status"), String.class),
                        field(name("w", "created_at"), OffsetDateTime.class)
                )
                .from(table(name("business", "waitlist_entries")).as("w"))
                .join(table(name("business", "customers")).as("c"))
                .on(field(name("c", "tenant_id")).eq(field(name("w", "tenant_id"))).and(field(name("c", "id")).eq(field(name("w", "customer_id")))))
                .join(table(name("business", "services")).as("s"))
                .on(field(name("s", "tenant_id")).eq(field(name("w", "tenant_id"))).and(field(name("s", "id")).eq(field(name("w", "service_id")))))
                .leftJoin(table(name("business", "staff")).as("st"))
                .on(field(name("st", "tenant_id")).eq(field(name("w", "tenant_id"))).and(field(name("st", "id")).eq(field(name("w", "staff_id")))))
                .where(
                        field(name("w", "tenant_id"), Long.class).eq(tenantId),
                        field(name("w", "id"), Long.class).eq(id)
                )
                .fetchOne();

        return Optional.ofNullable(r).map(this::mapToDto);
    }

    public List<WaitlistEntryDto> listActive(Long tenantId) {
        return dsl.select(
                        field(name("w", "id"), Long.class),
                        field(name("w", "tenant_id"), Long.class),
                        field(name("w", "customer_id"), Long.class),
                        field(name("c", "full_name"), String.class).as("customer_name"),
                        field(name("c", "phone_e164"), String.class).as("customer_phone"),
                        field(name("w", "service_id"), Long.class),
                        field(name("s", "name"), String.class).as("service_name"),
                        field(name("w", "staff_id"), Long.class),
                        field(name("st", "name"), String.class).as("staff_name"),
                        field(name("w", "earliest_date"), LocalDate.class),
                        field(name("w", "latest_date"), LocalDate.class),
                        field(name("w", "notes"), String.class),
                        field(name("w", "status"), String.class),
                        field(name("w", "created_at"), OffsetDateTime.class)
                )
                .from(table(name("business", "waitlist_entries")).as("w"))
                .join(table(name("business", "customers")).as("c"))
                .on(field(name("c", "tenant_id")).eq(field(name("w", "tenant_id"))).and(field(name("c", "id")).eq(field(name("w", "customer_id")))))
                .join(table(name("business", "services")).as("s"))
                .on(field(name("s", "tenant_id")).eq(field(name("w", "tenant_id"))).and(field(name("s", "id")).eq(field(name("w", "service_id")))))
                .leftJoin(table(name("business", "staff")).as("st"))
                .on(field(name("st", "tenant_id")).eq(field(name("w", "tenant_id"))).and(field(name("st", "id")).eq(field(name("w", "staff_id")))))
                .where(
                        field(name("w", "tenant_id"), Long.class).eq(tenantId),
                        field(name("w", "status"), String.class).eq("waiting")
                )
                .orderBy(field(name("w", "created_at"), OffsetDateTime.class).asc())
                .fetch(this::mapToDto);
    }

    public void updateStatus(Long tenantId, Long id, String status) {
        dsl.update(table(name("business", "waitlist_entries")))
                .set(field(name("status"), String.class), status)
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("id"), Long.class).eq(id)
                )
                .execute();
    }

    private WaitlistEntryDto mapToDto(Record r) {
        if (r == null) return null;
        OffsetDateTime created = r.get(field(name("w", "created_at"), OffsetDateTime.class));
        return new WaitlistEntryDto(
                r.get(field(name("w", "id"), Long.class)),
                r.get(field(name("w", "tenant_id"), Long.class)),
                r.get(field(name("w", "customer_id"), Long.class)),
                r.get(field(name("customer_name"), String.class)),
                r.get(field(name("customer_phone"), String.class)),
                r.get(field(name("w", "service_id"), Long.class)),
                r.get(field(name("service_name"), String.class)),
                r.get(field(name("w", "staff_id"), Long.class)),
                r.get(field(name("staff_name"), String.class)),
                r.get(field(name("w", "earliest_date"), LocalDate.class)),
                r.get(field(name("w", "latest_date"), LocalDate.class)),
                r.get(field(name("w", "notes"), String.class)),
                r.get(field(name("w", "status"), String.class)),
                created != null ? created.toInstant() : Instant.now()
        );
    }
}
