package com.booking.core.internal.security;

import com.booking.core.api.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 1. If user is authenticated in session, extract their tenant_id
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                if (principal.getTenantId() != null) {
                    TenantContext.setTenantId(principal.getTenantId());
                }
                if (principal.getId() != null) {
                    TenantContext.setUserId(principal.getId());
                }
            } else {
                // 2. Allow X-Tenant-ID header for API calls / tests
                String headerTenantId = request.getHeader("X-Tenant-ID");
                if (headerTenantId != null && !headerTenantId.isBlank()) {
                    try {
                        TenantContext.setTenantId(Long.parseLong(headerTenantId.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
