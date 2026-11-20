package com.booking.core.internal.web;

import com.booking.core.api.dto.LoginRequest;
import com.booking.core.api.dto.SignupRequest;
import com.booking.core.api.dto.TenantDto;
import com.booking.core.api.dto.UserDto;
import com.booking.core.internal.security.UserPrincipal;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/biz/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<?> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest
    ) {
        // 1. Check if slug exists
        if (tenantRepository.findBySlug(request.slug()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "SLUG_TAKEN",
                    "message", "This practice URL slug is already taken. Please choose another."
            ));
        }

        // 2. Check if email exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "EMAIL_EXISTS",
                    "message", "An account with this email already exists. Please sign in instead."
            ));
        }

        // 3. Load template settings
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

        // 4. Create tenant
        TenantDto tenant = tenantRepository.create(
                request.slug(),
                request.practiceName(),
                request.vertical(),
                "Asia/Kolkata",
                "INR",
                15,
                settingsJson
        );

        // 5. Create owner user
        UserDto user = userRepository.create(
                tenant.id(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.ownerName(),
                "owner"
        );

        // 6. Log the user in to create session
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        userRepository.updateLastLogin(user.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "user", user,
                "tenant", tenant
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        if (principal.isPlatformAdmin() || principal.getTenantId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        userRepository.updateLastLogin(principal.getId());
        UserDto user = userRepository.findById(principal.getTenantId(), principal.getId())
                .orElseThrow(() -> new IllegalStateException("User record not found"));

        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (principal.getTenantId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return userRepository.findById(principal.getTenantId(), principal.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
