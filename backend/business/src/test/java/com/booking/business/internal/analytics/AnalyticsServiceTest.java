package com.booking.business.internal.analytics;

import com.booking.business.api.dto.TrackerAppointmentDto;
import com.booking.business.internal.scheduling.AppointmentRepository;
import com.booking.core.api.TenantLookupService;
import com.booking.core.api.dto.TenantDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {

    @Test
    void testCsvExportFormatsProperly() {
        AnalyticsRepository analyticsRepo = mock(AnalyticsRepository.class);
        AppointmentRepository apptRepo = mock(AppointmentRepository.class);
        TenantLookupService tenantLookup = mock(TenantLookupService.class);

        when(tenantLookup.findById(1L)).thenReturn(Optional.of(new TenantDto(
                1L, "demo", "Smile Clinic", "dental", "UTC", "INR", 30, "{}", null, true, Instant.now()
        )));

        TrackerAppointmentDto row = new TrackerAppointmentDto(
                101L,
                55L,
                "Alice Smith",
                "+1234567890",
                10L,
                "Dental Cleaning, Polish",
                20L,
                "Dr. Priya",
                OffsetDateTime.parse("2026-11-20T10:00:00Z"),
                OffsetDateTime.parse("2026-11-20T10:45:00Z"),
                "completed",
                150000,
                150000,
                0L,
                "happy",
                "Teeth cleaned",
                "Regular checkup"
        );

        when(apptRepo.listTracker(eq(1L), any(), any(), isNull(), isNull(), isNull(), eq(5000)))
                .thenReturn(List.of(row));

        AnalyticsService service = new AnalyticsService(analyticsRepo, apptRepo, tenantLookup);

        String csv = service.generateCsv(1L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 22));

        assertNotNull(csv);
        assertTrue(csv.contains("Appointment ID,Date,Start Time"));
        assertTrue(csv.contains("Alice Smith"));
        assertTrue(csv.contains("\"Dental Cleaning, Polish\"")); // Escaped due to comma!
        assertTrue(csv.contains("1500.00")); // 150000 paise = 1500.00 INR
    }
}
