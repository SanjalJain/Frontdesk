package com.booking.business.internal.setup;

import com.booking.business.api.dto.ServiceDto;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
public class ServiceRepository {

    private final DSLContext dsl;

    public ServiceRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public ServiceDto create(
            Long tenantId,
            String name,
            int durationMin,
            int bufferAfterMin,
            int pricePaise
    ) {
        var record = dsl.insertInto(table(name("business", "services")))
                .columns(
                        field(name("tenant_id"), Long.class),
                        field(name("name"), String.class),
                        field(name("duration_min"), Short.class),
                        field(name("buffer_after_min"), Short.class),
                        field(name("price_paise"), Integer.class)
                )
                .values(
                        tenantId,
                        name.trim(),
                        (short) durationMin,
                        (short) bufferAfterMin,
                        pricePaise
                )
                .returning(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("name"), String.class),
                        field(name("duration_min"), Short.class),
                        field(name("buffer_after_min"), Short.class),
                        field(name("price_paise"), Integer.class),
                        field(name("is_active"), Boolean.class)
                )
                .fetchOne();

        return mapToDto(record);
    }

    public List<ServiceDto> listByTenant(Long tenantId) {
        return dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("name"), String.class),
                        field(name("duration_min"), Short.class),
                        field(name("buffer_after_min"), Short.class),
                        field(name("price_paise"), Integer.class),
                        field(name("is_active"), Boolean.class)
                )
                .from(table(name("business", "services")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("is_active"), Boolean.class).isTrue()
                )
                .orderBy(field(name("name"), String.class).asc())
                .fetch(this::mapToDto);
    }

    public Optional<ServiceDto> findById(Long tenantId, Long id) {
        var record = dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("name"), String.class),
                        field(name("duration_min"), Short.class),
                        field(name("buffer_after_min"), Short.class),
                        field(name("price_paise"), Integer.class),
                        field(name("is_active"), Boolean.class)
                )
                .from(table(name("business", "services")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("id"), Long.class).eq(id)
                )
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToDto);
    }

    private ServiceDto mapToDto(Record r) {
        if (r == null) return null;
        return new ServiceDto(
                r.get(field(name("id"), Long.class)),
                r.get(field(name("tenant_id"), Long.class)),
                r.get(field(name("name"), String.class)),
                r.get(field(name("duration_min"), Short.class)).intValue(),
                r.get(field(name("buffer_after_min"), Short.class)).intValue(),
                r.get(field(name("price_paise"), Integer.class)),
                Boolean.TRUE.equals(r.get(field(name("is_active"), Boolean.class)))
        );
    }
}
