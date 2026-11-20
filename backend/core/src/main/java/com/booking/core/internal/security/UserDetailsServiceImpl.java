package com.booking.core.internal.security;

import org.jooq.DSLContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static org.jooq.impl.DSL.*;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final DSLContext dsl;

    public UserDetailsServiceImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String cleanEmail = username.trim().toLowerCase();

        // 1. Check platform_admins
        var adminRecord = dsl.select(
                        field(name("id"), Long.class),
                        field(name("email"), String.class),
                        field(name("password_hash"), String.class)
                )
                .from(table(name("core", "platform_admins")))
                .where(lower(field(name("email"), String.class)).eq(cleanEmail))
                .fetchOne();

        if (adminRecord != null) {
            return new UserPrincipal(
                    adminRecord.value1(),
                    null,
                    adminRecord.value2(),
                    adminRecord.value3(),
                    "Platform Administrator",
                    "platform_admin",
                    true,
                    true
            );
        }

        // 2. Check tenant users
        var userRecord = dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("email"), String.class),
                        field(name("password_hash"), String.class),
                        field(name("full_name"), String.class),
                        field(name("role"), String.class),
                        field(name("is_active"), Boolean.class)
                )
                .from(table(name("core", "users")))
                .where(lower(field(name("email"), String.class)).eq(cleanEmail))
                .fetchOne();

        if (userRecord != null) {
            return new UserPrincipal(
                    userRecord.value1(),
                    userRecord.value2(),
                    userRecord.value3(),
                    userRecord.value4(),
                    userRecord.value5(),
                    userRecord.value6(),
                    false,
                    Boolean.TRUE.equals(userRecord.value7())
            );
        }

        throw new UsernameNotFoundException("User not found with email: " + username);
    }
}
