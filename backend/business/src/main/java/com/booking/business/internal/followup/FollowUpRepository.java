package com.booking.business.internal.followup;

import com.booking.business.api.dto.FollowUpCallDto;
import com.booking.business.api.dto.UpdateFollowUpRequest;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.jooq.impl.DSL.*;

@Repository
public class FollowUpRepository {

    private final DSLContext dsl;

    public FollowUpRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<FollowUpCallDto> listFollowUps(Long tenantId) {
        return dsl.select(
                        field(name("v", "appointment_id"), Long.class),
                        field(name("v", "customer_id"), Long.class),
                        field(name("c", "full_name"), String.class),
                        field(name("c", "phone_e164"), String.class),
                        field(name("c", "email"), String.class),
                        field(name("s", "name"), String.class),
                        field(name("st", "name"), String.class),
                        field(name("v", "last_visit_at"), OffsetDateTime.class),
                        field(name("a", "reaction"), String.class),
                        field(name("a", "reaction_note"), String.class),
                        field(name("a", "outcome"), String.class),
                        field(name("v", "followup_status"), String.class),
                        field(name("v", "followup_due_on"), LocalDate.class)
                )
                .from(table(name("business", "v_followup_call_list")).as("v"))
                .join(table(name("business", "customers")).as("c"))
                .on(field(name("c", "id")).eq(field(name("v", "customer_id"))))
                .join(table(name("business", "appointments")).as("a"))
                .on(field(name("a", "id")).eq(field(name("v", "appointment_id"))))
                .leftJoin(table(name("business", "services")).as("s"))
                .on(field(name("s", "id")).eq(field(name("a", "service_id"))))
                .leftJoin(table(name("business", "staff")).as("st"))
                .on(field(name("st", "id")).eq(field(name("a", "staff_id"))))
                .where(field(name("v", "tenant_id"), Long.class).eq(tenantId))
                .orderBy(field(name("v", "last_visit_at")).desc())
                .fetch(this::mapRecordToDto);
    }

    public boolean updateFollowUp(Long tenantId, Long appointmentId, UpdateFollowUpRequest req) {
        var update = dsl.update(table(name("business", "appointments")))
                .set(field(name("followup_status"), String.class), req.status().toLowerCase())
                .set(field(name("updated_at"), OffsetDateTime.class), OffsetDateTime.now());

        if (req.followupDueOn() != null) {
            update = update.set(field(name("followup_due_on"), LocalDate.class), req.followupDueOn());
        }

        int affected = update
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .and(field(name("id"), Long.class).eq(appointmentId))
                .execute();

        return affected > 0;
    }

    private FollowUpCallDto mapRecordToDto(Record r) {
        return new FollowUpCallDto(
                r.get(field(name("v", "appointment_id"), Long.class)),
                r.get(field(name("v", "customer_id"), Long.class)),
                r.get(field(name("c", "full_name"), String.class)),
                r.get(field(name("c", "phone_e164"), String.class)),
                r.get(field(name("c", "email"), String.class)),
                r.get(field(name("s", "name"), String.class)),
                r.get(field(name("st", "name"), String.class)),
                r.get(field(name("v", "last_visit_at"), OffsetDateTime.class)),
                r.get(field(name("a", "reaction"), String.class)),
                r.get(field(name("a", "reaction_note"), String.class)),
                r.get(field(name("a", "outcome"), String.class)),
                r.get(field(name("v", "followup_status"), String.class)),
                r.get(field(name("v", "followup_due_on"), LocalDate.class))
        );
    }
}
