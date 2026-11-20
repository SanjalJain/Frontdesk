package com.booking.business.internal.setup;

import com.booking.business.api.dto.WorkingHoursDto;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.jooq.impl.DSL.*;

@Repository
public class WorkingHoursRepository {

    private final DSLContext dsl;

    public WorkingHoursRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public void setStaffWorkingHours(
            Long tenantId,
            Long staffId,
            List<WorkingHoursDto.WorkingHoursRequest> hours
    ) {
        dsl.deleteFrom(table(name("business", "working_hours")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("staff_id"), Long.class).eq(staffId)
                )
                .execute();

        if (hours != null && !hours.isEmpty()) {
            var insert = dsl.insertInto(table(name("business", "working_hours")))
                    .columns(
                            field(name("tenant_id"), Long.class),
                            field(name("staff_id"), Long.class),
                            field(name("weekday"), Short.class),
                            field(name("start_time"), LocalTime.class),
                            field(name("end_time"), LocalTime.class)
                    );
            for (var h : hours) {
                insert = insert.values(tenantId, staffId, (short) h.weekday(), h.startTime(), h.endTime());
            }
            insert.execute();
        }
    }

    public List<WorkingHoursDto> listByStaff(Long tenantId, Long staffId) {
        return dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("staff_id"), Long.class),
                        field(name("weekday"), Short.class),
                        field(name("start_time"), LocalTime.class),
                        field(name("end_time"), LocalTime.class)
                )
                .from(table(name("business", "working_hours")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("staff_id"), Long.class).eq(staffId)
                )
                .orderBy(
                        field(name("weekday"), Short.class).asc(),
                        field(name("start_time"), LocalTime.class).asc()
                )
                .fetch(this::mapToDto);
    }

    public List<WorkingHoursDto> listByTenant(Long tenantId) {
        return dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("staff_id"), Long.class),
                        field(name("weekday"), Short.class),
                        field(name("start_time"), LocalTime.class),
                        field(name("end_time"), LocalTime.class)
                )
                .from(table(name("business", "working_hours")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .orderBy(
                        field(name("staff_id"), Long.class).asc(),
                        field(name("weekday"), Short.class).asc(),
                        field(name("start_time"), LocalTime.class).asc()
                )
                .fetch(this::mapToDto);
    }

    private WorkingHoursDto mapToDto(Record r) {
        if (r == null) return null;
        return new WorkingHoursDto(
                r.get(field(name("id"), Long.class)),
                r.get(field(name("tenant_id"), Long.class)),
                r.get(field(name("staff_id"), Long.class)),
                r.get(field(name("weekday"), Short.class)).intValue(),
                r.get(field(name("start_time"), LocalTime.class)),
                r.get(field(name("end_time"), LocalTime.class))
        );
    }
}
