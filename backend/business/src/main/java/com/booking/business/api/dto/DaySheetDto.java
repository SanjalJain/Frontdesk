package com.booking.business.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DaySheetDto(
        LocalDate date,
        int totalAppointments,
        int completedCount,
        int arrivedCount,
        int inProgressCount,
        int pendingOrBookedCount,
        List<AppointmentDto> appointments,
        Map<String, List<AppointmentDto>> appointmentsByStaff
) {}
