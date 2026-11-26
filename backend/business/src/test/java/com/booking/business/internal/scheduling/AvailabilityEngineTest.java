package com.booking.business.internal.scheduling;

import com.booking.business.api.dto.AvailableSlotDto;
import com.booking.business.api.dto.ServiceDto;
import com.booking.business.api.dto.StaffDto;
import com.booking.business.api.dto.TimeOffDto;
import com.booking.business.api.dto.WorkingHoursDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AvailabilityEngineTest {

    private AvailabilityEngine engine;
    private ZoneId zone;

    @BeforeEach
    void setUp() {
        engine = new AvailabilityEngine();
        zone = ZoneId.of("Asia/Kolkata");
    }

    @Test
    @DisplayName("Should return slots during working hours when no conflicts exist")
    void shouldReturnSlotsDuringWorkingHours() {
        // Future date, say next Monday
        LocalDate date = LocalDate.now().plusWeeks(1);
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.plusDays(1);
        }

        ServiceDto service = new ServiceDto(1L, 1L, "Consultation", 30, 0, 50000, true);
        StaffDto staff = new StaffDto(10L, 1L, "Dr. Alice", "Dentist", "#10B981", true, true, 1, List.of(1L));

        WorkingHoursDto mondayHours = new WorkingHoursDto(
                100L, 1L, 10L, 1, // 1 = Monday
                LocalTime.of(9, 0), LocalTime.of(11, 0)
        );

        List<AvailableSlotDto> slots = engine.calculateAvailableSlots(
                service,
                date,
                zone,
                List.of(staff),
                Map.of(10L, List.of(mondayHours)),
                Collections.emptyList(),
                Collections.emptyList(),
                30 // 30 min step
        );

        // 9:00-9:30, 9:30-10:00, 10:00-10:30, 10:30-11:00 -> 4 slots
        assertEquals(4, slots.size());
        assertEquals(LocalTime.of(9, 0), slots.get(0).startsAt().toLocalTime());
        assertEquals(LocalTime.of(9, 30), slots.get(0).endsAt().toLocalTime());
        assertEquals(LocalTime.of(10, 30), slots.get(3).startsAt().toLocalTime());
        assertEquals(LocalTime.of(11, 0), slots.get(3).endsAt().toLocalTime());
    }

    @Test
    @DisplayName("Should exclude slots that overlap with existing appointments")
    void shouldExcludeAppointmentConflicts() {
        LocalDate date = LocalDate.now().plusWeeks(1);
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.plusDays(1);
        }

        ServiceDto service = new ServiceDto(1L, 1L, "Consultation", 30, 0, 50000, true);
        StaffDto staff = new StaffDto(10L, 1L, "Dr. Alice", "Dentist", "#10B981", true, true, 1, List.of(1L));

        WorkingHoursDto mondayHours = new WorkingHoursDto(
                100L, 1L, 10L, 1,
                LocalTime.of(9, 0), LocalTime.of(11, 0)
        );

        // Existing appointment from 09:30 to 10:00
        OffsetDateTime apptStart = date.atTime(9, 30).atZone(zone).toOffsetDateTime();
        OffsetDateTime apptEnd = date.atTime(10, 0).atZone(zone).toOffsetDateTime();
        var existingAppt = new AvailabilityEngine.AppointmentWindow(10L, apptStart, apptEnd);

        List<AvailableSlotDto> slots = engine.calculateAvailableSlots(
                service,
                date,
                zone,
                List.of(staff),
                Map.of(10L, List.of(mondayHours)),
                Collections.emptyList(),
                List.of(existingAppt),
                30
        );

        // Slots at 9:00, 10:00, 10:30 (9:30 slot excluded)
        assertEquals(3, slots.size());
        assertFalse(slots.stream().anyMatch(s -> s.startsAt().toLocalTime().equals(LocalTime.of(9, 30))));
    }

    @Test
    @DisplayName("Should exclude slots that overlap with staff or clinic time-off")
    void shouldExcludeTimeOffConflicts() {
        LocalDate date = LocalDate.now().plusWeeks(1);
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.plusDays(1);
        }

        ServiceDto service = new ServiceDto(1L, 1L, "Consultation", 30, 0, 50000, true);
        StaffDto staff = new StaffDto(10L, 1L, "Dr. Alice", "Dentist", "#10B981", true, true, 1, List.of(1L));

        WorkingHoursDto mondayHours = new WorkingHoursDto(
                100L, 1L, 10L, 1,
                LocalTime.of(9, 0), LocalTime.of(11, 0)
        );

        // Whole clinic closed from 10:00 onwards
        Instant leaveStart = date.atTime(10, 0).atZone(zone).toInstant();
        Instant leaveEnd = date.atTime(18, 0).atZone(zone).toInstant();
        TimeOffDto clinicTimeOff = new TimeOffDto(1L, 1L, null, leaveStart, leaveEnd, "Staff meeting");

        List<AvailableSlotDto> slots = engine.calculateAvailableSlots(
                service,
                date,
                zone,
                List.of(staff),
                Map.of(10L, List.of(mondayHours)),
                List.of(clinicTimeOff),
                Collections.emptyList(),
                30
        );

        // Only 09:00 and 09:30 should be available
        assertEquals(2, slots.size());
        assertEquals(LocalTime.of(9, 0), slots.get(0).startsAt().toLocalTime());
        assertEquals(LocalTime.of(9, 30), slots.get(1).startsAt().toLocalTime());
    }

    @Test
    @DisplayName("Should return empty list if day of week has no working hours")
    void shouldReturnEmptyForNonWorkingDay() {
        LocalDate sunday = LocalDate.now().plusWeeks(1);
        while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
            sunday = sunday.plusDays(1);
        }

        ServiceDto service = new ServiceDto(1L, 1L, "Consultation", 30, 0, 50000, true);
        StaffDto staff = new StaffDto(10L, 1L, "Dr. Alice", "Dentist", "#10B981", true, true, 1, List.of(1L));

        // Only works on Monday (1)
        WorkingHoursDto mondayHours = new WorkingHoursDto(
                100L, 1L, 10L, 1,
                LocalTime.of(9, 0), LocalTime.of(17, 0)
        );

        List<AvailableSlotDto> slots = engine.calculateAvailableSlots(
                service,
                sunday,
                zone,
                List.of(staff),
                Map.of(10L, List.of(mondayHours)),
                Collections.emptyList(),
                Collections.emptyList(),
                30
        );

        assertTrue(slots.isEmpty());
    }
}
