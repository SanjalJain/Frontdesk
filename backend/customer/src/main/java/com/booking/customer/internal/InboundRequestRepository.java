package com.booking.customer.internal;

import com.booking.customer.api.dto.InboundRequestDto;
import com.booking.customer.api.dto.SubmitInboundRequest;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
public class InboundRequestRepository {

    private final DSLContext dsl;

    public InboundRequestRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public InboundRequestDto create(Long tenantId, SubmitInboundRequest req, String phoneE164) {
        OffsetDateTime now = OffsetDateTime.now();

        var rec = dsl.insertInto(table(name("customer", "inbound_requests")))
                .columns(
                        field(name("tenant_id"), Long.class),
                        field(name("type"), String.class),
                        field(name("customer_name"), String.class),
                        field(name("phone_e164"), String.class),
                        field(name("email"), String.class),
                        field(name("service_id"), Long.class),
                        field(name("preferred_date"), LocalDate.class),
                        field(name("preferred_time_window"), String.class),
                        field(name("concern"), String.class),
                        field(name("status"), String.class),
                        field(name("created_at"), OffsetDateTime.class),
                        field(name("updated_at"), OffsetDateTime.class)
                )
                .values(
                        tenantId,
                        req.type().toLowerCase(),
                        req.customerName().trim(),
                        phoneE164.trim(),
                        req.email() != null && !req.email().isBlank() ? req.email().trim().toLowerCase() : null,
                        req.serviceId(),
                        req.preferredDate(),
                        req.preferredTimeWindow(),
                        req.concern(),
                        "pending",
                        now,
                        now
                )
                .returning(field(name("id"), Long.class))
                .fetchOne();

        Long id = rec != null ? rec.value1() : null;
        return findById(tenantId, id).orElseThrow(() -> new IllegalStateException("Failed to load created inbound request"));
    }

    public Optional<InboundRequestDto> findById(Long tenantId, Long id) {
        var rec = dsl.select(
                        field(name("r", "id"), Long.class),
                        field(name("r", "tenant_id"), Long.class),
                        field(name("r", "type"), String.class),
                        field(name("r", "customer_name"), String.class),
                        field(name("r", "phone_e164"), String.class),
                        field(name("r", "email"), String.class),
                        field(name("r", "service_id"), Long.class),
                        field(name("s", "name"), String.class).as("service_name"),
                        field(name("r", "preferred_date"), LocalDate.class),
                        field(name("r", "preferred_time_window"), String.class),
                        field(name("r", "concern"), String.class),
                        field(name("r", "status"), String.class),
                        field(name("r", "converted_appointment_id"), Long.class),
                        field(name("r", "created_at"), OffsetDateTime.class)
                )
                .from(table(name("customer", "inbound_requests")).as("r"))
                .leftJoin(table(name("business", "services")).as("s"))
                .on(field(name("s", "id")).eq(field(name("r", "service_id"))))
                .where(field(name("r", "tenant_id"), Long.class).eq(tenantId))
                .and(field(name("r", "id"), Long.class).eq(id))
                .fetchOne();

        return Optional.ofNullable(rec).map(this::mapRecordToDto);
    }

    public List<InboundRequestDto> listPendingByTenant(Long tenantId) {
        return dsl.select(
                        field(name("r", "id"), Long.class),
                        field(name("r", "tenant_id"), Long.class),
                        field(name("r", "type"), String.class),
                        field(name("r", "customer_name"), String.class),
                        field(name("r", "phone_e164"), String.class),
                        field(name("r", "email"), String.class),
                        field(name("r", "service_id"), Long.class),
                        field(name("s", "name"), String.class).as("service_name"),
                        field(name("r", "preferred_date"), LocalDate.class),
                        field(name("r", "preferred_time_window"), String.class),
                        field(name("r", "concern"), String.class),
                        field(name("r", "status"), String.class),
                        field(name("r", "converted_appointment_id"), Long.class),
                        field(name("r", "created_at"), OffsetDateTime.class)
                )
                .from(table(name("customer", "inbound_requests")).as("r"))
                .leftJoin(table(name("business", "services")).as("s"))
                .on(field(name("s", "id")).eq(field(name("r", "service_id"))))
                .where(field(name("r", "tenant_id"), Long.class).eq(tenantId))
                .and(field(name("r", "status"), String.class).eq("pending"))
                .orderBy(field(name("r", "created_at")).desc())
                .fetch(this::mapRecordToDto);
    }

    public boolean updateStatus(Long tenantId, Long id, String status, Long convertedAppointmentId, Long handledBy) {
        OffsetDateTime now = OffsetDateTime.now();

        var update = dsl.update(table(name("customer", "inbound_requests")))
                .set(field(name("status"), String.class), status.toLowerCase())
                .set(field(name("handled_by"), Long.class), handledBy)
                .set(field(name("handled_at"), OffsetDateTime.class), now)
                .set(field(name("updated_at"), OffsetDateTime.class), now);

        if (convertedAppointmentId != null) {
            update = update.set(field(name("converted_appointment_id"), Long.class), convertedAppointmentId);
        }

        int affected = update
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .and(field(name("id"), Long.class).eq(id))
                .execute();

        return affected > 0;
    }

    private InboundRequestDto mapRecordToDto(Record r) {
        OffsetDateTime createdAt = r.get(field(name("r", "created_at"), OffsetDateTime.class));
        return new InboundRequestDto(
                r.get(field(name("r", "id"), Long.class)),
                r.get(field(name("r", "tenant_id"), Long.class)),
                r.get(field(name("r", "type"), String.class)),
                r.get(field(name("r", "customer_name"), String.class)),
                r.get(field(name("r", "phone_e164"), String.class)),
                r.get(field(name("r", "email"), String.class)),
                r.get(field(name("r", "service_id"), Long.class)),
                r.get(field(name("service_name"), String.class)),
                r.get(field(name("r", "preferred_date"), LocalDate.class)),
                r.get(field(name("r", "preferred_time_window"), String.class)),
                r.get(field(name("r", "concern"), String.class)),
                r.get(field(name("r", "status"), String.class)),
                r.get(field(name("r", "converted_appointment_id"), Long.class)),
                createdAt != null ? createdAt.toInstant() : null
        );
    }
}
