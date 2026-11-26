package com.booking.business.internal.analytics;

import com.booking.business.api.dto.AnalyticsSummaryDto;
import com.booking.business.api.dto.TrackerAppointmentDto;
import com.booking.business.internal.scheduling.AppointmentRepository;
import com.booking.core.api.TenantLookupService;
import com.booking.core.api.dto.TenantDto;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final AppointmentRepository appointmentRepository;
    private final TenantLookupService tenantLookupService;

    public AnalyticsService(
            AnalyticsRepository analyticsRepository,
            AppointmentRepository appointmentRepository,
            TenantLookupService tenantLookupService
    ) {
        this.analyticsRepository = analyticsRepository;
        this.appointmentRepository = appointmentRepository;
        this.tenantLookupService = tenantLookupService;
    }

    public AnalyticsSummaryDto getSummary(Long tenantId, LocalDate fromDate, LocalDate toDate) {
        ZoneId zoneId = getTenantZone(tenantId);
        LocalDate start = fromDate != null ? fromDate : LocalDate.now(zoneId).minusDays(30);
        LocalDate end = toDate != null ? toDate : LocalDate.now(zoneId);

        OffsetDateTime fromTz = start.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime toTz = end.atTime(LocalTime.MAX).atZone(zoneId).toOffsetDateTime();

        return analyticsRepository.getSummary(tenantId, fromTz, toTz, zoneId);
    }

    public String generateCsv(Long tenantId, LocalDate fromDate, LocalDate toDate) {
        ZoneId zoneId = getTenantZone(tenantId);
        LocalDate start = fromDate != null ? fromDate : LocalDate.now(zoneId).minusDays(30);
        LocalDate end = toDate != null ? toDate : LocalDate.now(zoneId);

        OffsetDateTime fromTz = start.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime toTz = end.atTime(LocalTime.MAX).atZone(zoneId).toOffsetDateTime();

        List<TrackerAppointmentDto> rows = appointmentRepository.listTracker(
                tenantId,
                fromTz,
                toTz,
                null,
                null,
                null,
                5000
        );

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Header
        pw.println("Appointment ID,Date,Start Time,End Time,Customer Name,Customer Phone,Staff,Service,Status,Billed (INR),Paid (INR),Balance (INR),Reaction,Outcome,Notes");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (TrackerAppointmentDto a : rows) {
            String dateStr = a.startsAt() != null ? a.startsAt().atZoneSameInstant(zoneId).format(dateFormatter) : "";
            String startStr = a.startsAt() != null ? a.startsAt().atZoneSameInstant(zoneId).format(timeFormatter) : "";
            String endStr = a.endsAt() != null ? a.endsAt().atZoneSameInstant(zoneId).format(timeFormatter) : "";

            double billedInr = a.pricePaise() / 100.0;
            double paidInr = a.paidPaise() / 100.0;
            double balanceInr = a.customerBalancePaise() / 100.0;

            pw.printf(
                    "%d,%s,%s,%s,%s,%s,%s,%s,%s,%.2f,%.2f,%.2f,%s,%s,%s%n",
                    a.id(),
                    escapeCsv(dateStr),
                    escapeCsv(startStr),
                    escapeCsv(endStr),
                    escapeCsv(a.customerName()),
                    escapeCsv(a.customerPhone()),
                    escapeCsv(a.staffName() != null ? a.staffName() : "Unassigned"),
                    escapeCsv(a.serviceName()),
                    escapeCsv(a.status()),
                    billedInr,
                    paidInr,
                    balanceInr,
                    escapeCsv(a.reaction() != null ? a.reaction() : ""),
                    escapeCsv(a.outcome() != null ? a.outcome() : ""),
                    escapeCsv(a.note() != null ? a.note() : "")
            );
        }

        pw.flush();
        return sw.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private ZoneId getTenantZone(Long tenantId) {
        return tenantLookupService.findById(tenantId)
                .map(TenantDto::timezone)
                .map(tz -> {
                    try {
                        return ZoneId.of(tz);
                    } catch (Exception e) {
                        return ZoneId.of("UTC");
                    }
                })
                .orElse(ZoneId.of("UTC"));
    }
}
