package com.booking.business.internal.scheduling;

import com.booking.business.api.dto.AvailableSlotDto;
import com.booking.business.api.dto.ServiceDto;
import com.booking.business.api.dto.StaffDto;
import com.booking.business.api.dto.TimeOffDto;
import com.booking.business.api.dto.WorkingHoursDto;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;

@Component
public class AvailabilityEngine {

    public record AppointmentWindow(
            Long staffId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {}

    /**
     * Pure calculation: computes all available slots for a given service on a given date.
     */
    public List<AvailableSlotDto> calculateAvailableSlots(
            ServiceDto service,
            LocalDate date,
            ZoneId zoneId,
            List<StaffDto> eligibleStaff,
            Map<Long, List<WorkingHoursDto>> workingHoursByStaff,
            List<TimeOffDto> timeOffList,
            List<AppointmentWindow> existingAppointments,
            int slotStepMinutes
    ) {
        if (eligibleStaff == null || eligibleStaff.isEmpty() || service == null) {
            return Collections.emptyList();
        }

        int weekday = date.getDayOfWeek().getValue(); // 1 = Monday ... 7 = Sunday
        int duration = service.durationMin();
        int buffer = service.bufferAfterMin();
        int step = slotStepMinutes > 0 ? slotStepMinutes : 15;
        Instant now = Instant.now();

        List<AvailableSlotDto> availableSlots = new ArrayList<>();

        for (StaffDto staff : eligibleStaff) {
            if (!staff.isBookable() || !staff.isActive()) {
                continue;
            }

            List<WorkingHoursDto> hours = workingHoursByStaff.getOrDefault(staff.id(), Collections.emptyList());
            for (WorkingHoursDto wh : hours) {
                if (wh.weekday() != weekday) {
                    continue;
                }

                OffsetDateTime blockStart = date.atTime(wh.startTime()).atZone(zoneId).toOffsetDateTime();
                OffsetDateTime blockEnd = date.atTime(wh.endTime()).atZone(zoneId).toOffsetDateTime();

                OffsetDateTime candidateStart = blockStart;

                while (!candidateStart.plusMinutes(duration).isAfter(blockEnd)) {
                    OffsetDateTime candidateEnd = candidateStart.plusMinutes(duration);
                    OffsetDateTime busyUntil = candidateEnd.plusMinutes(buffer);

                    // If candidate slot is in the past for today, skip it
                    if (candidateStart.toInstant().isAfter(now)) {
                        boolean overlapsTimeOff = checkTimeOffOverlap(staff.id(), candidateStart, busyUntil, timeOffList);
                        boolean overlapsAppointment = checkAppointmentOverlap(staff.id(), candidateStart, busyUntil, existingAppointments);

                        if (!overlapsTimeOff && !overlapsAppointment) {
                            availableSlots.add(new AvailableSlotDto(
                                    staff.id(),
                                    staff.name(),
                                    staff.color(),
                                    candidateStart,
                                    candidateEnd
                            ));
                        }
                    }

                    candidateStart = candidateStart.plusMinutes(step);
                }
            }
        }

        // Sort by start time, then staff name
        availableSlots.sort(Comparator.comparing(AvailableSlotDto::startsAt).thenComparing(AvailableSlotDto::staffName));
        return availableSlots;
    }

    private boolean checkTimeOffOverlap(
            Long staffId,
            OffsetDateTime slotStart,
            OffsetDateTime busyUntil,
            List<TimeOffDto> timeOffList
    ) {
        if (timeOffList == null || timeOffList.isEmpty()) {
            return false;
        }

        Instant startInst = slotStart.toInstant();
        Instant endInst = busyUntil.toInstant();

        for (TimeOffDto to : timeOffList) {
            // Whole-clinic time off (staffId == null) or this staff member
            if (to.staffId() == null || to.staffId().equals(staffId)) {
                // Overlap condition: startInst < to.endsAt && endInst > to.startsAt
                if (startInst.isBefore(to.endsAt()) && endInst.isAfter(to.startsAt())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkAppointmentOverlap(
            Long staffId,
            OffsetDateTime slotStart,
            OffsetDateTime busyUntil,
            List<AppointmentWindow> appointments
    ) {
        if (appointments == null || appointments.isEmpty()) {
            return false;
        }

        for (AppointmentWindow appt : appointments) {
            if (appt.staffId() != null && appt.staffId().equals(staffId)) {
                if (slotStart.isBefore(appt.endsAt()) && busyUntil.isAfter(appt.startsAt())) {
                    return true;
                }
            }
        }
        return false;
    }
}
