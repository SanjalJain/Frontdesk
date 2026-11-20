package com.booking.core.internal.tenancy;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static org.jooq.impl.DSL.*;

@Repository
public class PlatformAdminRepository {

    private final DSLContext dsl;

    public PlatformAdminRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Long create(String email, String passwordHash) {
        var record = dsl.insertInto(table(name("core", "platform_admins")))
                .columns(
                        field(name("email"), String.class),
                        field(name("password_hash"), String.class)
                )
                .values(email.trim().toLowerCase(), passwordHash)
                .returning(field(name("id"), Long.class))
                .fetchOne();

        return record != null ? record.get(field(name("id"), Long.class)) : null;
    }

    public boolean existsByEmail(String email) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(table(name("core", "platform_admins")))
                        .where(lower(field(name("email"), String.class)).eq(email.trim().toLowerCase()))
        );
    }
}
