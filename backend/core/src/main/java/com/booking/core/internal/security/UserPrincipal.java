package com.booking.core.internal.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Long tenantId; // null for platform admins
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final String role;
    private final boolean isPlatformAdmin;
    private final boolean active;

    public UserPrincipal(
            Long id,
            Long tenantId,
            String email,
            String passwordHash,
            String fullName,
            String role,
            boolean isPlatformAdmin,
            boolean active
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.isPlatformAdmin = isPlatformAdmin;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public boolean isPlatformAdmin() {
        return isPlatformAdmin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (isPlatformAdmin) {
            return List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
