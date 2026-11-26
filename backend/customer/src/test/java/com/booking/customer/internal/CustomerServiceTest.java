package com.booking.customer.internal;

import com.booking.core.api.event.OutboxEventPublisher;
import com.booking.customer.api.dto.InboundRequestDto;
import com.booking.customer.api.dto.PublicTenantProfileDto;
import com.booking.customer.api.dto.SubmitInboundRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    @Test
    void rejectsSubmissionWhenHoneypotIsFilled() {
        PublicProfileRepository profileRepo = mock(PublicProfileRepository.class);
        InboundRequestRepository reqRepo = mock(InboundRequestRepository.class);
        RateLimiterService rateLimiter = mock(RateLimiterService.class);
        PhoneNormalizationService phoneService = new PhoneNormalizationService();
        OutboxEventPublisher outbox = mock(OutboxEventPublisher.class);

        CustomerService service = new CustomerService(profileRepo, reqRepo, rateLimiter, phoneService, outbox);

        SubmitInboundRequest spamReq = new SubmitInboundRequest(
                "appointment",
                "Spam Bot",
                "+919876543210",
                "spam@bot.com",
                1L,
                LocalDate.now().plusDays(1),
                "morning",
                "Spam comment",
                "I am a spam bot filling hidden field"
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.submitRequest("demo", spamReq, "127.0.0.1")
        );
    }

    @Test
    void acceptsLegitimateSubmissionAndEmitsEvent() {
        PublicProfileRepository profileRepo = mock(PublicProfileRepository.class);
        InboundRequestRepository reqRepo = mock(InboundRequestRepository.class);
        RateLimiterService rateLimiter = mock(RateLimiterService.class);
        PhoneNormalizationService phoneService = new PhoneNormalizationService();
        OutboxEventPublisher outbox = mock(OutboxEventPublisher.class);

        when(rateLimiter.tryAcquire("127.0.0.1")).thenReturn(true);
        when(profileRepo.findBySlug("dr-priya")).thenReturn(Optional.of(new PublicTenantProfileDto(
                1L, "dr-priya", "Dr. Priya Dental", "dental", "Asia/Kolkata", "INR", 15, null,
                List.of(), List.of(), Map.of()
        )));

        InboundRequestDto mockDto = new InboundRequestDto(
                10L, 1L, "appointment", "Rahul Sharma", "+919876543210",
                "rahul@gmail.com", 2L, "Scaling", LocalDate.now().plusDays(2), "morning",
                "Need scaling", "pending", null, Instant.now()
        );

        when(reqRepo.create(eq(1L), any(), eq("+919876543210"))).thenReturn(mockDto);

        CustomerService service = new CustomerService(profileRepo, reqRepo, rateLimiter, phoneService, outbox);

        SubmitInboundRequest validReq = new SubmitInboundRequest(
                "appointment",
                "Rahul Sharma",
                "+919876543210",
                "rahul@gmail.com",
                2L,
                LocalDate.now().plusDays(2),
                "morning",
                "Need scaling",
                null // Honeypot clean
        );

        InboundRequestDto result = service.submitRequest("dr-priya", validReq, "127.0.0.1");

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("Rahul Sharma", result.customerName());
        verify(outbox, times(1)).enqueue(any());
    }
}
