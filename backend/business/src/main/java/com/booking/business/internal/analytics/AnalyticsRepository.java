package com.booking.business.internal.analytics;

import com.booking.business.api.dto.*;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.jooq.impl.DSL.*;

@Repository
public class AnalyticsRepository {

    private final DSLContext dsl;

    public AnalyticsRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public AnalyticsSummaryDto getSummary(Long tenantId, OffsetDateTime from, OffsetDateTime to, ZoneId zoneId) {
        // 1. Total revenue collected
        Long revenue = dsl.select(coalesce(sum(field(name("amount_paise"), Long.class)), 0L))
                .from(table(name("business", "payments")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .and(field(name("received_at"), OffsetDateTime.class).ge(from))
                .and(field(name("received_at"), OffsetDateTime.class).le(to))
                .fetchOne(0, Long.class);
        long totalRevenue = revenue != null ? revenue : 0L;

        // 2. Total billed
        Long billed = dsl.select(coalesce(sum(field(name("price_paise"), Long.class)), 0L))
                .from(table(name("business", "appointments")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .and(field(name("starts_at"), OffsetDateTime.class).ge(from))
                .and(field(name("starts_at"), OffsetDateTime.class).le(to))
                .and(field(name("status"), String.class).eq("completed"))
                .fetchOne(0, Long.class);
        long totalBilled = billed != null ? billed : 0L;

        // 3. Outstanding balance (all customers with positive balance)
        Long outstanding = dsl.select(coalesce(sum(field(name("balance_paise"), Long.class)), 0L))
                .from(table(name("business", "v_customer_balance")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .and(field(name("balance_paise"), Long.class).gt(0L))
                .fetchOne(0, Long.class);
        long outstandingBalance = outstanding != null ? outstanding : 0L;

        // 4. Booking counts by status
        var statusCounts = dsl.select(
                        field(name("status"), String.class),
                        count()
                )
                .from(table(name("business", "appointments")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .and(field(name("starts_at"), OffsetDateTime.class).ge(from))
                .and(field(name("starts_at"), OffsetDateTime.class).le(to))
                .groupBy(field(name("status"), String.class))
                .fetchMap(
                        r -> r.get(0, String.class),
                        r -> r.get(1, Integer.class)
                );

        int completed = statusCounts.getOrDefault("completed", 0);
        int cancelled = statusCounts.getOrDefault("cancelled", 0);
        int noShow = statusCounts.getOrDefault("no_show", 0);
        int totalBookings = statusCounts.values().stream().mapToInt(Integer::intValue).sum();

        double cancellationRate = totalBookings > 0 ? (cancelled * 100.0 / totalBookings) : 0.0;
        double noShowRate = totalBookings > 0 ? (noShow * 100.0 / totalBookings) : 0.0;

        // 5. Top Services
        List<TopServiceDto> topServices = dsl.select(
                        field(name("s", "id"), Long.class),
                        field(name("s", "name"), String.class),
                        count(field(name("a", "id"))),
                        coalesce(sum(field(name("a", "price_paise"), Long.class)), 0L)
                )
                .from(table(name("business", "appointments")).as("a"))
                .join(table(name("business", "services")).as("s"))
                .on(field(name("s", "id")).eq(field(name("a", "service_id"))))
                .where(field(name("a", "tenant_id"), Long.class).eq(tenantId))
                .and(field(name("a", "starts_at"), OffsetDateTime.class).ge(from))
                .and(field(name("a", "starts_at"), OffsetDateTime.class).le(to))
                .and(field(name("a", "status"), String.class).eq("completed"))
                .groupBy(field(name("s", "id")), field(name("s", "name")))
                .orderBy(count(field(name("a", "id"))).desc())
                .limit(10)
                .fetch(r -> new TopServiceDto(
                        r.value1(),
                        r.value2(),
                        r.value3(),
                        r.value4()
                ));

        // 6. Staff Performance
        List<StaffPerformanceDto> staffPerformance = dsl.select(
                        field(name("st", "id"), Long.class),
                        field(name("st", "name"), String.class),
                        count(field(name("a", "id"))),
                        count(case_().when(field(name("a", "status"), String.class).eq("completed"), 1).otherwise((Integer) null)),
                        coalesce(sum(case_().when(field(name("a", "status"), String.class).eq("completed"), field(name("a", "price_paise"), Long.class)).otherwise(0L)), 0L)
                )
                .from(table(name("business", "appointments")).as("a"))
                .join(table(name("business", "staff")).as("st"))
                .on(field(name("st", "id")).eq(field(name("a", "staff_id"))))
                .where(field(name("a", "tenant_id"), Long.class).eq(tenantId))
                .and(field(name("a", "starts_at"), OffsetDateTime.class).ge(from))
                .and(field(name("a", "starts_at"), OffsetDateTime.class).le(to))
                .groupBy(field(name("st", "id")), field(name("st", "name")))
                .orderBy(count(field(name("a", "id"))).desc())
                .fetch(r -> new StaffPerformanceDto(
                        r.value1(),
                        r.value2(),
                        r.value3(),
                        r.value4(),
                        r.value5()
                ));

        // 7. Daily Metrics
        List<DailyMetricDto> dailyMetrics = dsl.select(
                        sql("({0} AT TIME ZONE {1})::date", field(name("a", "starts_at"), OffsetDateTime.class), inline(zoneId.getId())).as("day_date"),
                        count(field(name("a", "id"))),
                        count(case_().when(field(name("a", "status"), String.class).eq("completed"), 1).otherwise((Integer) null)),
                        coalesce(sum(case_().when(field(name("a", "status"), String.class).eq("completed"), field(name("a", "price_paise"), Long.class)).otherwise(0L)), 0L)
                )
                .from(table(name("business", "appointments")).as("a"))
                .where(field(name("a", "tenant_id"), Long.class).eq(tenantId))
                .and(field(name("a", "starts_at"), OffsetDateTime.class).ge(from))
                .and(field(name("a", "starts_at"), OffsetDateTime.class).le(to))
                .groupBy(sql("day_date"))
                .orderBy(sql("day_date").asc())
                .fetch(r -> new DailyMetricDto(
                        LocalDate.parse(r.get("day_date").toString()),
                        r.get(1, Integer.class),
                        r.get(2, Integer.class),
                        r.get(3, Long.class)
                ));

        // 8. Payment Method distribution
        Map<String, MethodBreakdownDto> paymentMethods = new LinkedHashMap<>();
        dsl.select(
                        field(name("method"), String.class),
                        count(),
                        coalesce(sum(field(name("amount_paise"), Long.class)), 0L)
                )
                .from(table(name("business", "payments")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .and(field(name("received_at"), OffsetDateTime.class).ge(from))
                .and(field(name("received_at"), OffsetDateTime.class).le(to))
                .groupBy(field(name("method"), String.class))
                .forEach(r -> {
                    String method = r.value1();
                    paymentMethods.put(method, new MethodBreakdownDto(method, r.value2(), r.value3()));
                });

        return new AnalyticsSummaryDto(
                totalRevenue,
                totalBilled,
                outstandingBalance,
                totalBookings,
                completed,
                cancelled,
                noShow,
                Math.round(cancellationRate * 10.0) / 10.0,
                Math.round(noShowRate * 10.0) / 10.0,
                topServices,
                staffPerformance,
                dailyMetrics,
                paymentMethods
        );
    }
}
