package com.booking.core.internal.web;

import com.booking.core.api.dto.CreateTenantRequest;
import com.booking.core.api.dto.LoginRequest;
import com.booking.core.api.dto.TenantDto;
import com.booking.core.internal.security.UserPrincipal;
import com.booking.core.internal.tenancy.PlatformAdminRepository;
import com.booking.core.internal.tenancy.TenantRepository;
import com.booking.core.internal.tenancy.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class PlatformAdminController {

    private final AuthenticationManager authenticationManager;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PlatformAdminController(
            AuthenticationManager authenticationManager,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> adminLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        if (!principal.isPlatformAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        return ResponseEntity.ok(Map.of(
                "id", principal.getId(),
                "email", principal.getUsername(),
                "role", "platform_admin"
        ));
    }

    @PostMapping("/tenants")
    @Transactional
    public ResponseEntity<TenantDto> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        if (tenantRepository.findBySlug(request.slug()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Load vertical template JSON from classpath if available
        String settingsJson = "{}";
        try {
            var resource = new ClassPathResource("templates/verticals/" + request.vertical().toLowerCase() + ".json");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    settingsJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ignored) {
        }

        TenantDto tenant = tenantRepository.create(
                request.slug(),
                request.name(),
                request.vertical(),
                request.timezone(),
                request.currency(),
                request.slotMinutes() != null ? request.slotMinutes() : 15,
                settingsJson
        );

        // Provision initial owner user
        userRepository.create(
                tenant.id(),
                request.ownerEmail(),
                passwordEncoder.encode(request.ownerPassword()),
                request.ownerName(),
                "owner"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<TenantDto>> listTenants() {
        return ResponseEntity.ok(tenantRepository.listAll());
    }
}
