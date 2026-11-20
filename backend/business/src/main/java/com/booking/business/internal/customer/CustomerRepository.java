package com.booking.business.internal.customer;

import com.booking.business.api.dto.CustomerDto;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
public class CustomerRepository {

    private final DSLContext dsl;

    public CustomerRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public CustomerDto create(
            Long tenantId,
            String fullName,
            String phoneE164,
            String email,
            LocalDate dob,
            String notes,
            String intakeJson
    ) {
        var record = dsl.insertInto(table(name("business", "customers")))
                .columns(
                        field(name("tenant_id"), Long.class),
                        field(name("full_name"), String.class),
                        field(name("phone_e164"), String.class),
                        field(name("email"), String.class),
                        field(name("dob"), LocalDate.class),
                        field(name("notes"), String.class),
                        field(name("intake"), JSONB.class)
                )
                .values(
                        tenantId,
                        fullName.trim(),
                        phoneE164.trim(),
                        email != null ? email.trim().toLowerCase() : null,
                        dob,
                        notes,
                        JSONB.valueOf(intakeJson != null ? intakeJson : "{}")
                )
                .returning(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("full_name"), String.class),
                        field(name("phone_e164"), String.class),
                        field(name("email"), String.class),
                        field(name("dob"), LocalDate.class),
                        field(name("notes"), String.class),
                        field(name("intake"), JSONB.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .fetchOne();

        return mapToDto(record);
    }

    public Optional<CustomerDto> findById(Long tenantId, Long id) {
        var record = dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("full_name"), String.class),
                        field(name("phone_e164"), String.class),
                        field(name("email"), String.class),
                        field(name("dob"), LocalDate.class),
                        field(name("notes"), String.class),
                        field(name("intake"), JSONB.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .from(table(name("business", "customers")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("id"), Long.class).eq(id),
                        field(name("deleted_at")).isNull()
                )
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToDto);
    }

    public Optional<CustomerDto> findByPhone(Long tenantId, String phoneE164) {
        var record = dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("full_name"), String.class),
                        field(name("phone_e164"), String.class),
                        field(name("email"), String.class),
                        field(name("dob"), LocalDate.class),
                        field(name("notes"), String.class),
                        field(name("intake"), JSONB.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .from(table(name("business", "customers")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("phone_e164"), String.class).eq(phoneE164.trim()),
                        field(name("deleted_at")).isNull()
                )
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToDto);
    }

    public List<CustomerDto> search(Long tenantId, String query, int limit) {
        String cleanQuery = (query != null) ? query.trim() : "";
        var condition = field(name("tenant_id"), Long.class).eq(tenantId)
                .and(field(name("deleted_at")).isNull());

        if (!cleanQuery.isBlank()) {
            condition = condition.and(
                    field(name("phone_e164"), String.class).likeIgnoreCase("%" + cleanQuery + "%")
                            .or(lower(field(name("full_name"), String.class)).likeIgnoreCase("%" + cleanQuery.toLowerCase() + "%"))
            );
        }

        return dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("full_name"), String.class),
                        field(name("phone_e164"), String.class),
                        field(name("email"), String.class),
                        field(name("dob"), LocalDate.class),
                        field(name("notes"), String.class),
                        field(name("intake"), JSONB.class),
                        field(name("created_at"), OffsetDateTime.class)
                )
                .from(table(name("business", "customers")))
                .where(condition)
                .orderBy(field(name("created_at"), OffsetDateTime.class).desc())
                .limit(limit)
                .fetch(this::mapToDto);
    }

    private CustomerDto mapToDto(Record r) {
        if (r == null) return null;
        OffsetDateTime created = r.get(field(name("created_at"), OffsetDateTime.class));
        return new CustomerDto(
                r.get(field(name("id"), Long.class)),
                r.get(field(name("tenant_id"), Long.class)),
                r.get(field(name("full_name"), String.class)),
                r.get(field(name("phone_e164"), String.class)),
                r.get(field(name("email"), String.class)),
                r.get(field(name("dob"), LocalDate.class)),
                r.get(field(name("notes"), String.class)),
                r.get(field(name("intake"), JSONB.class)).data(),
                created != null ? created.toInstant() : Instant.now()
        );
    }
}
