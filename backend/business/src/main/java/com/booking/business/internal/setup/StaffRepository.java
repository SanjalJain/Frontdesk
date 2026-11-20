package com.booking.business.internal.setup;

import com.booking.business.api.dto.StaffDto;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
public class StaffRepository {

    private final DSLContext dsl;

    public StaffRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public StaffDto create(
            Long tenantId,
            String name,
            String title,
            String color,
            boolean isBookable,
            int sortOrder,
            List<Long> serviceIds
    ) {
        var record = dsl.insertInto(table(name("business", "staff")))
                .columns(
                        field(name("tenant_id"), Long.class),
                        field(name("name"), String.class),
                        field(name("title"), String.class),
                        field(name("color"), String.class),
                        field(name("is_bookable"), Boolean.class),
                        field(name("sort_order"), Short.class)
                )
                .values(
                        tenantId,
                        name.trim(),
                        title,
                        color != null ? color : "#A8895A",
                        isBookable,
                        (short) sortOrder
                )
                .returning(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("name"), String.class),
                        field(name("title"), String.class),
                        field(name("color"), String.class),
                        field(name("is_bookable"), Boolean.class),
                        field(name("is_active"), Boolean.class),
                        field(name("sort_order"), Short.class)
                )
                .fetchOne();

        Long staffId = record.get(field(name("id"), Long.class));
        if (serviceIds != null && !serviceIds.isEmpty()) {
            assignServices(tenantId, staffId, serviceIds);
        }

        return mapToDto(record, serviceIds != null ? serviceIds : Collections.emptyList());
    }

    @Transactional
    public void assignServices(Long tenantId, Long staffId, List<Long> serviceIds) {
        dsl.deleteFrom(table(name("business", "staff_services")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("staff_id"), Long.class).eq(staffId)
                )
                .execute();

        if (serviceIds != null && !serviceIds.isEmpty()) {
            var insert = dsl.insertInto(table(name("business", "staff_services")))
                    .columns(
                            field(name("tenant_id"), Long.class),
                            field(name("staff_id"), Long.class),
                            field(name("service_id"), Long.class)
                    );
            for (Long sId : serviceIds) {
                insert = insert.values(tenantId, staffId, sId);
            }
            insert.execute();
        }
    }

    public List<StaffDto> listByTenant(Long tenantId) {
        return dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("name"), String.class),
                        field(name("title"), String.class),
                        field(name("color"), String.class),
                        field(name("is_bookable"), Boolean.class),
                        field(name("is_active"), Boolean.class),
                        field(name("sort_order"), Short.class)
                )
                .from(table(name("business", "staff")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("is_active"), Boolean.class).isTrue()
                )
                .orderBy(field(name("sort_order"), Short.class).asc(), field(name("name"), String.class).asc())
                .fetch(r -> {
                    Long staffId = r.get(field(name("id"), Long.class));
                    List<Long> sIds = getServiceIdsForStaff(tenantId, staffId);
                    return mapToDto(r, sIds);
                });
    }

    public Optional<StaffDto> findById(Long tenantId, Long id) {
        var record = dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("name"), String.class),
                        field(name("title"), String.class),
                        field(name("color"), String.class),
                        field(name("is_bookable"), Boolean.class),
                        field(name("is_active"), Boolean.class),
                        field(name("sort_order"), Short.class)
                )
                .from(table(name("business", "staff")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("id"), Long.class).eq(id)
                )
                .fetchOne();

        if (record == null) return Optional.empty();
        List<Long> sIds = getServiceIdsForStaff(tenantId, id);
        return Optional.of(mapToDto(record, sIds));
    }

    public List<Long> getServiceIdsForStaff(Long tenantId, Long staffId) {
        return dsl.select(field(name("service_id"), Long.class))
                .from(table(name("business", "staff_services")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("staff_id"), Long.class).eq(staffId)
                )
                .fetch(field(name("service_id"), Long.class));
    }

    private StaffDto mapToDto(Record r, List<Long> serviceIds) {
        if (r == null) return null;
        return new StaffDto(
                r.get(field(name("id"), Long.class)),
                r.get(field(name("tenant_id"), Long.class)),
                r.get(field(name("name"), String.class)),
                r.get(field(name("title"), String.class)),
                r.get(field(name("color"), String.class)),
                Boolean.TRUE.equals(r.get(field(name("is_bookable"), Boolean.class))),
                Boolean.TRUE.equals(r.get(field(name("is_active"), Boolean.class))),
                r.get(field(name("sort_order"), Short.class)).intValue(),
                serviceIds
        );
    }
}
