package com.booking.customer.internal;

import com.booking.core.api.event.OutboxEventPublisher;
import com.booking.customer.api.InboundRequestBridgeService;
import com.booking.customer.api.dto.HandleInboundRequest;
import com.booking.customer.api.dto.InboundRequestDto;
import com.booking.customer.api.dto.PublicTenantProfileDto;
import com.booking.customer.api.dto.SubmitInboundRequest;
import com.booking.customer.api.event.InboundRequestSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService implements InboundRequestBridgeService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final PublicProfileRepository publicProfileRepository;
    private final InboundRequestRepository inboundRequestRepository;
    private final RateLimiterService rateLimiterService;
    private final PhoneNormalizationService phoneNormalizationService;
    private final OutboxEventPublisher outboxEventPublisher;

    public CustomerService(
            PublicProfileRepository publicProfileRepository,
            InboundRequestRepository inboundRequestRepository,
            RateLimiterService rateLimiterService,
            PhoneNormalizationService phoneNormalizationService,
            OutboxEventPublisher outboxEventPublisher
    ) {
        this.publicProfileRepository = publicProfileRepository;
        this.inboundRequestRepository = inboundRequestRepository;
        this.rateLimiterService = rateLimiterService;
        this.phoneNormalizationService = phoneNormalizationService;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    public Optional<PublicTenantProfileDto> getPublicProfile(String slug) {
        return publicProfileRepository.findBySlug(slug);
    }

    @Transactional
    public InboundRequestDto submitRequest(String slug, SubmitInboundRequest req, String clientIp) {
        // 1. Smart Honeypot Trap check
        if (req.hp_comment() != null && !req.hp_comment().isBlank()) {
            log.warn("Honeypot trap triggered for slug '{}' from IP: {}", slug, clientIp);
            throw new IllegalArgumentException("Invalid submission");
        }

        // 2. IP Rate Limiting check
        if (!rateLimiterService.tryAcquire(clientIp)) {
            log.warn("Rate limit exceeded for IP: {} on slug '{}'", clientIp, slug);
            throw new IllegalStateException("Too many requests from your IP. Please try again in a minute.");
        }

        // 3. Lookup tenant
        PublicTenantProfileDto profile = publicProfileRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Practice not found with slug: " + slug));

        // 4. Normalize phone number
        String phoneE164 = phoneNormalizationService.normalize(req.phone(), "IN");

        // 5. Create inbound request record
        InboundRequestDto created = inboundRequestRepository.create(profile.tenantId(), req, phoneE164);

        // 6. Publish outbox event for business front desk
        outboxEventPublisher.enqueue(new InboundRequestSubmittedEvent(
                created.tenantId(),
                created.id(),
                created.type(),
                created.customerName(),
                created.phoneE164(),
                Instant.now()
        ));

        log.info("Inbound request #{} ({}) submitted for tenant '{}'", created.id(), created.type(), profile.name());

        return created;
    }

    @Override
    public List<InboundRequestDto> listPendingRequests(Long tenantId) {
        return inboundRequestRepository.listPendingByTenant(tenantId);
    }

    @Override
    public Optional<InboundRequestDto> getRequest(Long tenantId, Long requestId) {
        return inboundRequestRepository.findById(tenantId, requestId);
    }

    @Override
    @Transactional
    public boolean handleRequest(Long tenantId, Long requestId, HandleInboundRequest request, Long handledBy) {
        return inboundRequestRepository.updateStatus(
                tenantId,
                requestId,
                request.status(),
                request.convertedAppointmentId(),
                handledBy
        );
    }
}
