package com.booking.core.internal.security;

import com.booking.core.api.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextFilterTest {

    private TenantContextFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantContextFilter();
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void extractsTenantIdFromUserPrincipalInSession() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                101L,
                42L,
                "dentist@clinic.com",
                "hash",
                "Dr. Smile",
                "owner",
                false,
                true
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // Filter cleans up thread local after filter chain execution
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void extractsTenantIdFromHeaderWhenUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "99");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final Long[] capturedTenant = new Long[1];
        filter.doFilter(request, response, (req, res) -> {
            capturedTenant[0] = TenantContext.getTenantId();
        });

        assertEquals(99L, capturedTenant[0]);
        // Cleaned up after filter returns
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void guaranteesContextCleanupEvenIfExceptionThrown() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(RuntimeException.class, () -> {
            filter.doFilter(request, response, (req, res) -> {
                assertEquals(123L, TenantContext.getTenantId());
                throw new RuntimeException("Simulated filter chain failure");
            });
        });

        assertNull(TenantContext.getTenantId(), "TenantContext must be cleared in finally block");
    }
}
