package com.booking.business.api.dto;

import java.util.List;
import java.util.Map;

public record AnalyticsSummaryDto(
        long totalRevenuePaise,
        long totalBilledPaise,
        long outstandingBalancePaise,
        int totalBookings,
        int completedBookings,
        int cancelledBookings,
        int noShowBookings,
        double cancellationRatePercent,
        double noShowRatePercent,
        List<TopServiceDto> topServices,
        List<StaffPerformanceDto> staffPerformance,
        List<DailyMetricDto> dailyMetrics,
        Map<String, MethodBreakdownDto> paymentMethods
) {}
