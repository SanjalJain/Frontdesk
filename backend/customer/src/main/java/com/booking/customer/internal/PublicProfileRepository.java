package com.booking.customer.internal;

import com.booking.customer.api.dto.PublicServiceDto;
import com.booking.customer.api.dto.PublicTenantProfileDto;
import com.booking.customer.api.dto.PublicWorkingHoursDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.*;

import static org.jooq.impl.DSL.*;

@Repository
public class PublicProfileRepository {

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public PublicProfileRepository(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
    }

    public Optional<PublicTenantProfileDto> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        String cleanSlug = slug.trim().toLowerCase();

        // 1. Try reading from cache
        var cached = dsl.select(
                        field(name("tenant_id"), Long.class),
                        field(name("slug"), String.class),
                        field(name("name"), String.class),
                        field(name("vertical"), String.class),
                        field(name("timezone"), String.class),
                        field(name("currency"), String.class),
                        field(name("slot_minutes"), Short.class),
                        field(name("logo_path"), String.class),
                        field(name("services_json"), JSONB.class),
                        field(name("hours_json"), JSONB.class),
                        field(name("branding_json"), JSONB.class)
                )
                .from(table(name("customer", "public_profile_cache")))
                .where(field(name("slug"), String.class).eq(cleanSlug))
                .fetchOne();

        if (cached != null) {
            return Optional.of(mapCachedRecord(cached));
        }

        // 2. If cache miss, sync from core and business schemas
        return syncFromSource(cleanSlug);
    }

    public Optional<PublicTenantProfileDto> syncFromSource(String slug) {
        // Find tenant in core.tenants
        var tenantRec = dsl.select(
                        field(name("id"), Long.class),
                        field(name("slug"), String.class),
                        field(name("name"), String.class),
                        field(name("vertical"), String.class),
                        field(name("timezone"), String.class),
                        field(name("currency"), String.class),
                        field(name("slot_minutes"), Integer.class),
                        field(name("logo_path"), String.class),
                        field(name("settings_json"), JSONB.class)
                )
                .from(table(name("core", "tenants")))
                .where(lower(field(name("slug"), String.class)).eq(slug))
                .and(field(name("is_active"), Boolean.class).isTrue())
                .fetchOne();

        if (tenantRec == null) {
            return Optional.empty();
        }

        Long tenantId = tenantRec.value1();
        String name = tenantRec.value3();
        String vertical = tenantRec.value4();
        String timezone = tenantRec.value5();
        String currency = tenantRec.value6();
        int slotMinutes = tenantRec.value7() != null ? tenantRec.value7() : 15;
        String logoPath = tenantRec.value8();
        JSONB settingsJson = tenantRec.value9();

        // Load active services
        List<PublicServiceDto> services = dsl.select(
                        field(name("id"), Long.class),
                        field(name("name"), String.class),
                        field(name("duration_min"), Short.class),
                        field(name("price_paise"), Integer.class)
                )
                .from(table(name("business", "services")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .and(field(name("is_active"), Boolean.class).isTrue())
                .orderBy(field(name("id")).asc())
                .fetch(r -> new PublicServiceDto(
                        r.value1(),
                        r.value2(),
                        r.value3() != null ? r.value3().intValue() : 30,
                        r.value4() != null ? r.value4() : 0
                ));

        // Load working hours
        List<PublicWorkingHoursDto> hours = dsl.select(
                        field(name("weekday"), Short.class),
                        field(name("start_time"), LocalTime.class),
                        field(name("end_time"), LocalTime.class)
                )
                .from(table(name("business", "working_hours")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .orderBy(field(name("weekday")).asc(), field(name("start_time")).asc())
                .fetch(r -> new PublicWorkingHoursDto(
                        r.value1() != null ? r.value1().intValue() : 1,
                        r.value2() != null ? r.value2().toString() : "09:00",
                        r.value3() != null ? r.value3().toString() : "18:00"
                ));

        Map<String, Object> branding = new LinkedHashMap<>();
        if (settingsJson != null && !settingsJson.data().isBlank()) {
            try {
                branding = objectMapper.readValue(settingsJson.data(), new TypeReference<>() {});
            } catch (Exception ignored) {
            }
        }

        try {
            String servicesJsonStr = objectMapper.writeValueAsString(services);
            String hoursJsonStr = objectMapper.writeValueAsString(hours);
            String brandingJsonStr = objectMapper.writeValueAsString(branding);

            // Upsert into customer.public_profile_cache
            dsl.insertInto(table(name("customer", "public_profile_cache")))
                    .columns(
                            field(name("tenant_id"), Long.class),
                            field(name("slug"), String.class),
                            field(name("name"), String.class),
                            field(name("vertical"), String.class),
                            field(name("timezone"), String.class),
                            field(name("currency"), String.class),
                            field(name("slot_minutes"), Short.class),
                            field(name("logo_path"), String.class),
                            field(name("services_json"), JSONB.class),
                            field(name("hours_json"), JSONB.class),
                            field(name("branding_json"), JSONB.class)
                    )
                    .values(
                            tenantId,
                            slug,
                            name,
                            vertical,
                            timezone,
                            currency != null ? currency : "INR",
                            (short) slotMinutes,
                            logoPath,
                            JSONB.valueOf(servicesJsonStr),
                            JSONB.valueOf(hoursJsonStr),
                            JSONB.valueOf(brandingJsonStr)
                    )
                    .onConflict(field(name("tenant_id")))
                    .doUpdate()
                    .set(field(name("slug"), String.class), slug)
                    .set(field(name("name"), String.class), name)
                    .set(field(name("vertical"), String.class), vertical)
                    .set(field(name("timezone"), String.class), timezone)
                    .set(field(name("currency"), String.class), currency != null ? currency : "INR")
                    .set(field(name("slot_minutes"), Short.class), (short) slotMinutes)
                    .set(field(name("logo_path"), String.class), logoPath)
                    .set(field(name("services_json"), JSONB.class), JSONB.valueOf(servicesJsonStr))
                    .set(field(name("hours_json"), JSONB.class), JSONB.valueOf(hoursJsonStr))
                    .set(field(name("branding_json"), JSONB.class), JSONB.valueOf(brandingJsonStr))
                    .execute();
        } catch (Exception ignored) {
        }

        return Optional.of(new PublicTenantProfileDto(
                tenantId,
                slug,
                name,
                vertical,
                timezone,
                currency != null ? currency : "INR",
                slotMinutes,
                logoPath,
                services,
                hours,
                branding
        ));
    }

    private PublicTenantProfileDto mapCachedRecord(Record r) {
        Long tenantId = r.get(field(name("tenant_id"), Long.class));
        String slug = r.get(field(name("slug"), String.class));
        String name = r.get(field(name("name"), String.class));
        String vertical = r.get(field(name("vertical"), String.class));
        String timezone = r.get(field(name("timezone"), String.class));
        String currency = r.get(field(name("currency"), String.class));
        Short slotMin = r.get(field(name("slot_minutes"), Short.class));
        String logoPath = r.get(field(name("logo_path"), String.class));
        JSONB servicesJson = r.get(field(name("services_json"), JSONB.class));
        JSONB hoursJson = r.get(field(name("hours_json"), JSONB.class));
        JSONB brandingJson = r.get(field(name("branding_json"), JSONB.class));

        List<PublicServiceDto> services = new ArrayList<>();
        if (servicesJson != null) {
            try {
                services = objectMapper.readValue(servicesJson.data(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }

        List<PublicWorkingHoursDto> hours = new ArrayList<>();
        if (hoursJson != null) {
            try {
                hours = objectMapper.readValue(hoursJson.data(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }

        Map<String, Object> branding = new LinkedHashMap<>();
        if (brandingJson != null) {
            try {
                branding = objectMapper.readValue(brandingJson.data(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }

        return new PublicTenantProfileDto(
                tenantId,
                slug,
                name,
                vertical,
                timezone,
                currency,
                slotMin != null ? slotMin.intValue() : 15,
                logoPath,
                services,
                hours,
                branding
        );
    }
}
