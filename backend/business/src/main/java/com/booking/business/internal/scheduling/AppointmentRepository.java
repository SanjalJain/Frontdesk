package com.booking.business.internal.scheduling;

import com.booking.business.api.dto.AppointmentDto;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
public class AppointmentRepository {

    private final DSLContext dsl;

    public AppointmentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public AppointmentDto create(
            Long tenantId,
            Long customerId,
            Long serviceId,
            Long staffId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String source,
            String requestNote,
            int pricePaise,
            Long createdBy
    ) {
        var record = dsl.insertInto(table(name("business", "appointments")))
                .columns(
                        field(name("tenant_id"), Long.class),
                        field(name("customer_id"), Long.class),
                        field(name("service_id"), Long.class),
                        field(name("staff_id"), Long.class),
                        field(name("starts_at"), OffsetDateTime.class),
                        field(name("ends_at"), OffsetDateTime.class),
                        field(name("status"), String.class),
                        field(name("source"), String.class),
                        field(name("request_note"), String.class),
                        field(name("price_paise"), Integer.class),
                        field(name("version"), Integer.class),
                        field(name("created_by"), Long.class)
                )
                .values(
                        tenantId,
                        customerId,
                        serviceId,
                        staffId,
                        startsAt,
                        endsAt,
                        "booked",
                        source != null ? source : "phone",
                        requestNote,
                        pricePaise,
                        0,
                        createdBy
                )
                .returning(field(name("id"), Long.class))
                .fetchOne();

        Long id = record.get(field(name("id"), Long.class));
        return findById(tenantId, id).orElseThrow(() -> new IllegalStateException("Failed to load created appointment"));
    }

    public Optional<AppointmentDto> findById(Long tenantId, Long id) {
        var r = baseSelect()
                .where(
                        field(name("a", "tenant_id"), Long.class).eq(tenantId),
                        field(name("a", "id"), Long.class).eq(id)
                )
                .fetchOne();

        return Optional.ofNullable(r).map(this::mapToDto);
    }

    public List<AppointmentDto> listByRange(Long tenantId, OffsetDateTime from, OffsetDateTime to, Long staffId) {
        var query = baseSelect()
                .where(
                        field(name("a", "tenant_id"), Long.class).eq(tenantId),
                        field(name("a", "starts_at"), OffsetDateTime.class).lt(to),
                        field(name("a", "ends_at"), OffsetDateTime.class).gt(from)
                );

        if (staffId != null) {
            query = (SelectConditionStep<Record>) query.and(field(name("a", "staff_id"), Long.class).eq(staffId));
        }

        return query.orderBy(field(name("a", "starts_at"), OffsetDateTime.class).asc())
                .fetch(this::mapToDto);
    }

    public List<AppointmentDto> listByCustomer(Long tenantId, Long customerId) {
        return baseSelect()
                .where(
                        field(name("a", "tenant_id"), Long.class).eq(tenantId),
                        field(name("a", "customer_id"), Long.class).eq(customerId)
                )
                .orderBy(field(name("a", "starts_at"), OffsetDateTime.class).desc())
                .fetch(this::mapToDto);
    }

    public List<AppointmentDto> listDaySheet(Long tenantId, LocalDate date, ZoneId zoneId) {
        OffsetDateTime startOfDay = date.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();

        return baseSelect()
                .where(
                        field(name("a", "tenant_id"), Long.class).eq(tenantId),
                        field(name("a", "starts_at"), OffsetDateTime.class).gte(startOfDay),
                        field(name("a", "starts_at"), OffsetDateTime.class).lt(endOfDay)
                )
                .orderBy(
                        field(name("st", "sort_order"), Short.class).asc().nullsLast(),
                        field(name("st", "name"), String.class).asc().nullsLast(),
                        field(name("a", "starts_at"), OffsetDateTime.class).asc()
                )
                .fetch(this::mapToDto);
    }

    public List<AvailabilityEngine.AppointmentWindow> listActiveWindowsForDate(
            Long tenantId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return dsl.select(
                        field(name("staff_id"), Long.class),
                        field(name("starts_at"), OffsetDateTime.class),
                        field(name("ends_at"), OffsetDateTime.class)
                )
                .from(table(name("business", "appointments")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("starts_at"), OffsetDateTime.class).lt(to),
                        field(name("ends_at"), OffsetDateTime.class).gt(from),
                        field(name("status"), String.class).in("booked", "confirmed", "arrived", "in_progress", "completed")
                )
                .fetch(r -> new AvailabilityEngine.AppointmentWindow(
                        r.get(field(name("staff_id"), Long.class)),
                        r.get(field(name("starts_at"), OffsetDateTime.class)),
                        r.get(field(name("ends_at"), OffsetDateTime.class))
                ));
    }

    public AppointmentDto reschedule(
            Long tenantId,
            Long id,
            Long newStaffId,
            OffsetDateTime newStartsAt,
            OffsetDateTime newEndsAt,
            int expectedVersion
    ) {
        var update = dsl.update(table(name("business", "appointments")))
                .set(field(name("starts_at"), OffsetDateTime.class), newStartsAt)
                .set(field(name("ends_at"), OffsetDateTime.class), newEndsAt)
                .set(field(name("version"), Integer.class), expectedVersion + 1)
                .set(field(name("updated_at"), OffsetDateTime.class), currentOffsetDateTime());

        if (newStaffId != null) {
            update = update.set(field(name("staff_id"), Long.class), newStaffId);
        }

        int updatedRows = update.where(
                field(name("tenant_id"), Long.class).eq(tenantId),
                field(name("id"), Long.class).eq(id),
                field(name("version"), Integer.class).eq(expectedVersion)
        ).execute();

        if (updatedRows == 0) {
            throw new AppointmentOptimisticLockException(
                    "Appointment has been modified or rescheduled by another user. Please refresh and try again."
            );
        }

        return findById(tenantId, id).orElseThrow(() -> new IllegalStateException("Appointment not found"));
    }

    public AppointmentDto updateStatus(
            Long tenantId,
            Long id,
            String newStatus,
            String note,
            String outcome,
            String reaction,
            String reactionNote,
            String cancelReason,
            String noShowReason,
            Integer expectedVersion
    ) {
        var update = dsl.update(table(name("business", "appointments")))
                .set(field(name("status"), String.class), newStatus)
                .set(field(name("updated_at"), OffsetDateTime.class), currentOffsetDateTime());

        OffsetDateTime now = OffsetDateTime.now();
        if ("arrived".equals(newStatus)) {
            update = update.set(field(name("checked_in_at"), OffsetDateTime.class), now);
        } else if ("in_progress".equals(newStatus)) {
            update = update.set(field(name("started_at"), OffsetDateTime.class), now);
        } else if ("completed".equals(newStatus)) {
            update = update.set(field(name("completed_at"), OffsetDateTime.class), now);
        }

        if (outcome != null) {
            update = update.set(field(name("outcome"), String.class), outcome);
        }
        if (reaction != null) {
            update = update.set(field(name("reaction"), String.class), reaction);
        }
        if (reactionNote != null) {
            update = update.set(field(name("reaction_note"), String.class), reactionNote);
        }
        if (cancelReason != null) {
            update = update.set(field(name("cancel_reason"), String.class), cancelReason);
        }
        if (noShowReason != null) {
            update = update.set(field(name("no_show_reason"), String.class), noShowReason);
        }

        if (expectedVersion != null) {
            update = update.set(field(name("version"), Integer.class), expectedVersion + 1);
        }

        var condition = field(name("tenant_id"), Long.class).eq(tenantId)
                .and(field(name("id"), Long.class).eq(id));

        if (expectedVersion != null) {
            condition = condition.and(field(name("version"), Integer.class).eq(expectedVersion));
        }

        int updatedRows = update.where(condition).execute();
        if (updatedRows == 0 && expectedVersion != null) {
            throw new AppointmentOptimisticLockException(
                    "Appointment status was modified concurrently. Please refresh and try again."
            );
        }

        return findById(tenantId, id).orElseThrow(() -> new IllegalStateException("Appointment not found"));
    }

    public List<TrackerAppointmentDto> listTracker(
            Long tenantId,
            OffsetDateTime from,
            OffsetDateTime to,
            Long staffId,
            String status,
            String search,
            int limit
    ) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(field(name("a", "tenant_id"), Long.class).eq(tenantId));

        if (from != null) {
            conditions.add(field(name("a", "starts_at"), OffsetDateTime.class).ge(from));
        }
        if (to != null) {
            conditions.add(field(name("a", "starts_at"), OffsetDateTime.class).le(to));
        }
        if (staffId != null) {
            conditions.add(field(name("a", "staff_id"), Long.class).eq(staffId));
        }
        if (status != null && !status.isBlank()) {
            conditions.add(field(name("a", "status"), String.class).eq(status.toLowerCase()));
        }
        if (search != null && !search.isBlank()) {
            String query = "%" + search.trim() + "%";
            conditions.add(
                    field(name("c", "full_name"), String.class).likeIgnoreCase(query)
                            .or(field(name("c", "phone_e164"), String.class).like(query))
            );
        }

        var paidPaiseSubquery = select(coalesce(sum(field(name("p", "amount_paise"), Integer.class)), 0))
                .from(table(name("business", "payments")).as("p"))
                .where(field(name("p", "tenant_id")).eq(field(name("a", "tenant_id"))))
                .and(field(name("p", "appointment_id")).eq(field(name("a", "id"))));

        return dsl.select(
                        field(name("a", "id"), Long.class),
                        field(name("a", "customer_id"), Long.class),
                        field(name("c", "full_name"), String.class).as("customer_name"),
                        field(name("c", "phone_e164"), String.class).as("customer_phone"),
                        field(name("a", "service_id"), Long.class),
                        field(name("s", "name"), String.class).as("service_name"),
                        field(name("a", "staff_id"), Long.class),
                        field(name("st", "name"), String.class).as("staff_name"),
                        field(name("a", "starts_at"), OffsetDateTime.class),
                        field(name("a", "ends_at"), OffsetDateTime.class),
                        field(name("a", "status"), String.class),
                        field(name("a", "price_paise"), Integer.class),
                        paidPaiseSubquery.asField("paid_paise"),
                        coalesce(field(name("bal", "balance_paise"), Long.class), 0L).as("customer_balance_paise"),
                        field(name("a", "reaction"), String.class),
                        field(name("a", "outcome"), String.class),
                        field(name("a", "request_note"), String.class)
                )
                .from(table(name("business", "appointments")).as("a"))
                .join(table(name("business", "customers")).as("c"))
                .on(field(name("c", "tenant_id")).eq(field(name("a", "tenant_id"))).and(field(name("c", "id")).eq(field(name("a", "customer_id")))))
                .join(table(name("business", "services")).as("s"))
                .on(field(name("s", "tenant_id")).eq(field(name("a", "tenant_id"))).and(field(name("s", "id")).eq(field(name("a", "service_id")))))
                .leftJoin(table(name("business", "staff")).as("st"))
                .on(field(name("st", "tenant_id")).eq(field(name("a", "tenant_id"))).and(field(name("st", "id")).eq(field(name("a", "staff_id")))))
                .leftJoin(table(name("business", "v_customer_balance")).as("bal"))
                .on(field(name("bal", "tenant_id")).eq(field(name("a", "tenant_id"))).and(field(name("bal", "customer_id")).eq(field(name("a", "customer_id")))))
                .where(conditions)
                .orderBy(field(name("a", "starts_at")).desc())
                .limit(limit > 0 ? limit : 200)
                .fetch(r -> new TrackerAppointmentDto(
                        r.get(field(name("a", "id"), Long.class)),
                        r.get(field(name("a", "customer_id"), Long.class)),
                        r.get(field(name("customer_name"), String.class)),
                        r.get(field(name("customer_phone"), String.class)),
                        r.get(field(name("a", "service_id"), Long.class)),
                        r.get(field(name("service_name"), String.class)),
                        r.get(field(name("a", "staff_id"), Long.class)),
                        r.get(field(name("staff_name"), String.class)),
                        r.get(field(name("a", "starts_at"), OffsetDateTime.class)),
                        r.get(field(name("a", "ends_at"), OffsetDateTime.class)),
                        r.get(field(name("a", "status"), String.class)),
                        r.get(field(name("a", "price_paise"), Integer.class)) != null ? r.get(field(name("a", "price_paise"), Integer.class)) : 0,
                        r.get(field(name("paid_paise"), Integer.class)) != null ? r.get(field(name("paid_paise"), Integer.class)) : 0,
                        r.get(field(name("customer_balance_paise"), Long.class)) != null ? r.get(field(name("customer_balance_paise"), Long.class)) : 0L,
                        r.get(field(name("a", "reaction"), String.class)),
                        r.get(field(name("a", "outcome"), String.class)),
                        r.get(field(name("a", "request_note"), String.class))
                ));
    }

    private SelectConditionStep<Record> baseSelect() {
        return dsl.select(
                        field(name("a", "id"), Long.class),
                        field(name("a", "tenant_id"), Long.class),
                        field(name("a", "customer_id"), Long.class),
                        field(name("c", "full_name"), String.class).as("customer_name"),
                        field(name("c", "phone_e164"), String.class).as("customer_phone"),
                        field(name("a", "service_id"), Long.class),
                        field(name("s", "name"), String.class).as("service_name"),
                        field(name("s", "duration_min"), Short.class).as("service_duration_min"),
                        field(name("a", "staff_id"), Long.class),
                        field(name("st", "name"), String.class).as("staff_name"),
                        field(name("st", "color"), String.class).as("staff_color"),
                        field(name("a", "starts_at"), OffsetDateTime.class),
                        field(name("a", "ends_at"), OffsetDateTime.class),
                        field(name("a", "status"), String.class),
                        field(name("a", "source"), String.class),
                        field(name("a", "request_note"), String.class),
                        field(name("a", "price_paise"), Integer.class),
                        field(name("a", "version"), Integer.class),
                        field(name("a", "checked_in_at"), OffsetDateTime.class),
                        field(name("a", "started_at"), OffsetDateTime.class),
                        field(name("a", "completed_at"), OffsetDateTime.class),
                        field(name("a", "outcome"), String.class),
                        field(name("a", "no_show_reason"), String.class),
                        field(name("a", "cancel_reason"), String.class),
                        field(name("a", "reaction"), String.class),
                        field(name("a", "reaction_note"), String.class),
                        field(name("a", "followup_status"), String.class),
                        field(name("a", "followup_due_on"), LocalDate.class),
                        field(name("a", "created_at"), OffsetDateTime.class),
                        field(name("a", "updated_at"), OffsetDateTime.class)
                )
                .from(table(name("business", "appointments")).as("a"))
                .join(table(name("business", "customers")).as("c"))
                .on(field(name("c", "tenant_id")).eq(field(name("a", "tenant_id"))).and(field(name("c", "id")).eq(field(name("a", "customer_id")))))
                .join(table(name("business", "services")).as("s"))
                .on(field(name("s", "tenant_id")).eq(field(name("a", "tenant_id"))).and(field(name("s", "id")).eq(field(name("a", "service_id")))))
                .leftJoin(table(name("business", "staff")).as("st"))
                .on(field(name("st", "tenant_id")).eq(field(name("a", "tenant_id"))).and(field(name("st", "id")).eq(field(name("a", "staff_id")))))
                .where(trueCondition());
    }

    private AppointmentDto mapToDto(Record r) {
        if (r == null) return null;
        OffsetDateTime created = r.get(field(name("a", "created_at"), OffsetDateTime.class));
        OffsetDateTime updated = r.get(field(name("a", "updated_at"), OffsetDateTime.class));
        Short dur = r.get(field(name("service_duration_min"), Short.class));

        return new AppointmentDto(
                r.get(field(name("a", "id"), Long.class)),
                r.get(field(name("a", "tenant_id"), Long.class)),
                r.get(field(name("a", "customer_id"), Long.class)),
                r.get(field(name("customer_name"), String.class)),
                r.get(field(name("customer_phone"), String.class)),
                r.get(field(name("a", "service_id"), Long.class)),
                r.get(field(name("service_name"), String.class)),
                dur != null ? dur.intValue() : 0,
                r.get(field(name("a", "staff_id"), Long.class)),
                r.get(field(name("staff_name"), String.class)),
                r.get(field(name("staff_color"), String.class)),
                r.get(field(name("a", "starts_at"), OffsetDateTime.class)),
                r.get(field(name("a", "ends_at"), OffsetDateTime.class)),
                r.get(field(name("a", "status"), String.class)),
                r.get(field(name("a", "source"), String.class)),
                r.get(field(name("a", "request_note"), String.class)),
                r.get(field(name("a", "price_paise"), Integer.class)),
                r.get(field(name("a", "version"), Integer.class)),
                r.get(field(name("a", "checked_in_at"), OffsetDateTime.class)),
                r.get(field(name("a", "started_at"), OffsetDateTime.class)),
                r.get(field(name("a", "completed_at"), OffsetDateTime.class)),
                r.get(field(name("a", "outcome"), String.class)),
                r.get(field(name("a", "no_show_reason"), String.class)),
                r.get(field(name("a", "cancel_reason"), String.class)),
                r.get(field(name("a", "reaction"), String.class)),
                r.get(field(name("a", "reaction_note"), String.class)),
                r.get(field(name("a", "followup_status"), String.class)),
                r.get(field(name("a", "followup_due_on"), LocalDate.class)),
                created != null ? created.toInstant() : Instant.now(),
                updated != null ? updated.toInstant() : Instant.now()
        );
    }
}
