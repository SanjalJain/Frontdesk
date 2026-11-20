package com.booking.core.internal.tenancy;

import com.booking.core.api.dto.UserDto;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
public class UserRepository {

    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public UserDto create(
            Long tenantId,
            String email,
            String passwordHash,
            String fullName,
            String role
    ) {
        var record = dsl.insertInto(table(name("core", "users")))
                .columns(
                        field(name("tenant_id"), Long.class),
                        field(name("email"), String.class),
                        field(name("password_hash"), String.class),
                        field(name("full_name"), String.class),
                        field(name("role"), String.class)
                )
                .values(
                        tenantId,
                        email.trim().toLowerCase(),
                        passwordHash,
                        fullName,
                        role != null ? role : "owner"
                )
                .returning(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("email"), String.class),
                        field(name("full_name"), String.class),
                        field(name("role"), String.class),
                        field(name("is_active"), Boolean.class),
                        field(name("last_login_at"), OffsetDateTime.class)
                )
                .fetchOne();

        return mapToDto(record);
    }

    public Optional<UserDto> findById(Long tenantId, Long id) {
        var record = dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("email"), String.class),
                        field(name("full_name"), String.class),
                        field(name("role"), String.class),
                        field(name("is_active"), Boolean.class),
                        field(name("last_login_at"), OffsetDateTime.class)
                )
                .from(table(name("core", "users")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("id"), Long.class).eq(id)
                )
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToDto);
    }

    public Optional<UserDto> findByEmail(String email) {
        var record = dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("email"), String.class),
                        field(name("full_name"), String.class),
                        field(name("role"), String.class),
                        field(name("is_active"), Boolean.class),
                        field(name("last_login_at"), OffsetDateTime.class)
                )
                .from(table(name("core", "users")))
                .where(lower(field(name("email"), String.class)).eq(email.trim().toLowerCase()))
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToDto);
    }

    public void updateLastLogin(Long id) {
        dsl.update(table(name("core", "users")))
                .set(field(name("last_login_at")), currentOffsetDateTime())
                .where(field(name("id"), Long.class).eq(id))
                .execute();
    }

    private UserDto mapToDto(Record r) {
        if (r == null) return null;
        OffsetDateTime lastLogin = r.get(field(name("last_login_at"), OffsetDateTime.class));
        return new UserDto(
                r.get(field(name("id"), Long.class)),
                r.get(field(name("tenant_id"), Long.class)),
                r.get(field(name("email"), String.class)),
                r.get(field(name("full_name"), String.class)),
                r.get(field(name("role"), String.class)),
                Boolean.TRUE.equals(r.get(field(name("is_active"), Boolean.class))),
                lastLogin != null ? lastLogin.toInstant() : null
        );
    }
}
