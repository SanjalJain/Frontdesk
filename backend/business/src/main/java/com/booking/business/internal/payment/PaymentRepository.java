package com.booking.business.internal.payment;

import com.booking.business.api.dto.CustomerBalanceDto;
import com.booking.business.api.dto.PaymentDto;
import com.booking.business.api.dto.RecordPaymentRequest;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
public class PaymentRepository {

    private final DSLContext dsl;

    public PaymentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public PaymentDto create(Long tenantId, RecordPaymentRequest req, Long receivedBy) {
        int year = Year.now().getValue();
        String prefix = "RCP-" + tenantId + "-" + year + "-";

        Integer count = dsl.selectCount()
                .from(table(name("business", "payments")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .and(field(name("receipt_no"), String.class).startsWith(prefix))
                .fetchOne(0, Integer.class);

        int seq = (count == null ? 0 : count) + 1;
        String receiptNo = String.format("RCP-%d-%d-%05d", tenantId, year, seq);

        OffsetDateTime now = OffsetDateTime.now();

        var record = dsl.insertInto(table(name("business", "payments")))
                .columns(
                        field(name("tenant_id"), Long.class),
                        field(name("customer_id"), Long.class),
                        field(name("appointment_id"), Long.class),
                        field(name("amount_paise"), Integer.class),
                        field(name("method"), String.class),
                        field(name("reference"), String.class),
                        field(name("receipt_no"), String.class),
                        field(name("received_by"), Long.class),
                        field(name("received_at"), OffsetDateTime.class),
                        field(name("note"), String.class)
                )
                .values(
                        tenantId,
                        req.customerId(),
                        req.appointmentId(),
                        req.amountPaise(),
                        req.method().toLowerCase(),
                        req.reference(),
                        receiptNo,
                        receivedBy,
                        now,
                        req.note()
                )
                .returning(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("customer_id"), Long.class),
                        field(name("appointment_id"), Long.class),
                        field(name("amount_paise"), Integer.class),
                        field(name("method"), String.class),
                        field(name("reference"), String.class),
                        field(name("receipt_no"), String.class),
                        field(name("received_by"), Long.class),
                        field(name("received_at"), OffsetDateTime.class),
                        field(name("note"), String.class)
                )
                .fetchOne();

        Long paymentId = record.get(field(name("id"), Long.class));
        return findById(tenantId, paymentId).orElseThrow(() -> new IllegalStateException("Failed to load newly created payment"));
    }

    public Optional<PaymentDto> findById(Long tenantId, Long id) {
        var rec = dsl.select(
                        field(name("p", "id"), Long.class),
                        field(name("p", "tenant_id"), Long.class),
                        field(name("p", "customer_id"), Long.class),
                        field(name("c", "full_name"), String.class),
                        field(name("c", "phone_e164"), String.class),
                        field(name("p", "appointment_id"), Long.class),
                        field(name("p", "amount_paise"), Integer.class),
                        field(name("p", "method"), String.class),
                        field(name("p", "reference"), String.class),
                        field(name("p", "receipt_no"), String.class),
                        field(name("p", "received_by"), Long.class),
                        field(name("u", "full_name"), String.class),
                        field(name("p", "received_at"), OffsetDateTime.class),
                        field(name("p", "note"), String.class)
                )
                .from(table(name("business", "payments")).as("p"))
                .join(table(name("business", "customers")).as("c"))
                .on(field(name("c", "id")).eq(field(name("p", "customer_id"))))
                .leftJoin(table(name("core", "users")).as("u"))
                .on(field(name("u", "id")).eq(field(name("p", "received_by"))))
                .where(field(name("p", "tenant_id"), Long.class).eq(tenantId))
                .and(field(name("p", "id"), Long.class).eq(id))
                .fetchOne();

        return Optional.ofNullable(rec).map(this::mapRecordToPaymentDto);
    }

    public List<PaymentDto> listPayments(
            Long tenantId,
            Long customerId,
            Long appointmentId,
            OffsetDateTime from,
            OffsetDateTime to,
            int limit
    ) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(field(name("p", "tenant_id"), Long.class).eq(tenantId));

        if (customerId != null) {
            conditions.add(field(name("p", "customer_id"), Long.class).eq(customerId));
        }
        if (appointmentId != null) {
            conditions.add(field(name("p", "appointment_id"), Long.class).eq(appointmentId));
        }
        if (from != null) {
            conditions.add(field(name("p", "received_at"), OffsetDateTime.class).ge(from));
        }
        if (to != null) {
            conditions.add(field(name("p", "received_at"), OffsetDateTime.class).le(to));
        }

        return dsl.select(
                        field(name("p", "id"), Long.class),
                        field(name("p", "tenant_id"), Long.class),
                        field(name("p", "customer_id"), Long.class),
                        field(name("c", "full_name"), String.class),
                        field(name("c", "phone_e164"), String.class),
                        field(name("p", "appointment_id"), Long.class),
                        field(name("p", "amount_paise"), Integer.class),
                        field(name("p", "method"), String.class),
                        field(name("p", "reference"), String.class),
                        field(name("p", "receipt_no"), String.class),
                        field(name("p", "received_by"), Long.class),
                        field(name("u", "full_name"), String.class),
                        field(name("p", "received_at"), OffsetDateTime.class),
                        field(name("p", "note"), String.class)
                )
                .from(table(name("business", "payments")).as("p"))
                .join(table(name("business", "customers")).as("c"))
                .on(field(name("c", "id")).eq(field(name("p", "customer_id"))))
                .leftJoin(table(name("core", "users")).as("u"))
                .on(field(name("u", "id")).eq(field(name("p", "received_by"))))
                .where(conditions)
                .orderBy(field(name("p", "received_at")).desc())
                .limit(limit > 0 ? limit : 100)
                .fetch(this::mapRecordToPaymentDto);
    }

    public CustomerBalanceDto getCustomerBalance(Long tenantId, Long customerId) {
        Long billed = dsl.select(coalesce(sum(field(name("price_paise"), Long.class)), 0L))
                .from(table(name("business", "appointments")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .and(field(name("customer_id"), Long.class).eq(customerId))
                .and(field(name("status"), String.class).eq("completed"))
                .fetchOne(0, Long.class);

        Long paid = dsl.select(coalesce(sum(field(name("amount_paise"), Long.class)), 0L))
                .from(table(name("business", "payments")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .and(field(name("customer_id"), Long.class).eq(customerId))
                .fetchOne(0, Long.class);

        long billedVal = billed != null ? billed : 0L;
        long paidVal = paid != null ? paid : 0L;
        long balance = billedVal - paidVal;

        return new CustomerBalanceDto(customerId, billedVal, paidVal, balance);
    }

    private PaymentDto mapRecordToPaymentDto(Record r) {
        return new PaymentDto(
                r.get(field(name("p", "id"), Long.class)),
                r.get(field(name("p", "tenant_id"), Long.class)),
                r.get(field(name("p", "customer_id"), Long.class)),
                r.get(field(name("c", "full_name"), String.class)),
                r.get(field(name("c", "phone_e164"), String.class)),
                r.get(field(name("p", "appointment_id"), Long.class)),
                r.get(field(name("p", "amount_paise"), Integer.class)),
                r.get(field(name("p", "method"), String.class)),
                r.get(field(name("p", "reference"), String.class)),
                r.get(field(name("p", "receipt_no"), String.class)),
                r.get(field(name("p", "received_by"), Long.class)),
                r.get(field(name("u", "full_name"), String.class)),
                r.get(field(name("p", "received_at"), OffsetDateTime.class)),
                r.get(field(name("p", "note"), String.class))
        );
    }
}
