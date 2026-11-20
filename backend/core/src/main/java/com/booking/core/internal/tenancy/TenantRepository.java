package com.booking.core.internal.tenancy;

import com.booking.core.api.dto.TenantDto;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
public class TenantRepository {

    private final DSLContext dsl;

    public TenantRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public TenantDto create(
            String slug,
            String name,
            String vertical,
            String timezone,
            String currency,
            int slotMinutes,
            String settingsJson
    ) {
        var record = dsl.insertInto(table(name("core", "tenants")))
                .columns(
                        field(name("slug"), String.class),
                        field(name("name"), String.class),
                        field(name("vertical"), String.class),
                        field(name("timezone"), String.class),
                        field(name("currency"), String.class),
                        field(name("slot_minutes"), Short.class),
                        field(name("settings"), JSONB.class)
                )
                .values(
                        slug,
                        name,
                        vertical,
                        timezone != null ? timezone : "Asia/Kolkata",
                        currency != null ? currency : "INR",
                        (short) slotMinutes,
                        JSONB.valueOf(settingsJson != null ? settingsJson : "{}")
                )
                .returning(
                        field(name("id"), Long.class),
                        field(name("slug"), String.class),
                        field(name("name"), String.class),
                        field(name("vertical"), String.class),
                        field(name("timezone"), String.class),
                        field(name("currency"), String.class),
                        field(name("slot_minutes"), Short.class),
                        field(name("settings"), JSONB.class),
                        field(name("logo_path"), String.class),
                        field(name("is_active"), Boolean.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .fetchOne();

        return mapToDto(record);
    }

    public Optional<TenantDto> findById(Long id) {
        var record = dsl.select(
                        field(name("id"), Long.class),
                        field(name("slug"), String.class),
                        field(name("name"), String.class),
                        field(name("vertical"), String.class),
                        field(name("timezone"), String.class),
                        field(name("currency"), String.class),
                        field(name("slot_minutes"), Short.class),
                        field(name("settings"), JSONB.class),
                        field(name("logo_path"), String.class),
                        field(name("is_active"), Boolean.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .from(table(name("core", "tenants")))
                .where(field(name("id"), Long.class).eq(id))
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToDto);
    }

    public Optional<TenantDto> findBySlug(String slug) {
        var record = dsl.select(
                        field(name("id"), Long.class),
                        field(name("slug"), String.class),
                        field(name("name"), String.class),
                        field(name("vertical"), String.class),
                        field(name("timezone"), String.class),
                        field(name("currency"), String.class),
                        field(name("slot_minutes"), Short.class),
                        field(name("settings"), JSONB.class),
                        field(name("logo_path"), String.class),
                        field(name("is_active"), Boolean.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .from(table(name("core", "tenants")))
                .where(field(name("slug"), String.class).eq(slug.trim().toLowerCase()))
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToDto);
    }

    public List<TenantDto> listAll() {
        return dsl.select(
                        field(name("id"), Long.class),
                        field(name("slug"), String.class),
                        field(name("name"), String.class),
                        field(name("vertical"), String.class),
                        field(name("timezone"), String.class),
                        field(name("currency"), String.class),
                        field(name("slot_minutes"), Short.class),
                        field(name("settings"), JSONB.class),
                        field(name("logo_path"), String.class),
                        field(name("is_active"), Boolean.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .from(table(name("core", "tenants")))
                .orderBy(field(name("id"), Long.class).desc())
                .fetch(this::mapToDto);
    }

    private TenantDto mapToDto(Record r) {
        if (r == null) return null;
        OffsetDateTime created = r.get(field(name("created_at"), OffsetDateTime.class));
        return new TenantDto(
                r.get(field(name("id"), Long.class)),
                r.get(field(name("slug"), String.class)),
                r.get(field(name("name"), String.class)),
                r.get(field(name("vertical"), String.class)),
                r.get(field(name("timezone"), String.class)),
                r.get(field(name("currency"), String.class)),
                r.get(field(name("slot_minutes"), Short.class)).intValue(),
                r.get(field(name("settings"), JSONB.class)).data(),
                r.get(field(name("logo_path"), String.class)),
                Boolean.TRUE.equals(r.get(field(name("is_active"), Boolean.class))),
                created != null ? created.toInstant() : Instant.now()
        );
    }
}
