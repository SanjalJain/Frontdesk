package com.booking.business.internal.payment;

import com.booking.business.api.dto.CustomerBalanceDto;
import com.booking.business.api.dto.PaymentDto;
import com.booking.business.api.dto.ReceiptDto;
import com.booking.business.api.dto.RecordPaymentRequest;
import com.booking.core.api.TenantLookupService;
import com.booking.core.api.dto.TenantDto;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TenantLookupService tenantLookupService;
    private final DSLContext dsl;

    public PaymentService(
            PaymentRepository paymentRepository,
            TenantLookupService tenantLookupService,
            DSLContext dsl
    ) {
        this.paymentRepository = paymentRepository;
        this.tenantLookupService = tenantLookupService;
        this.dsl = dsl;
    }

    @Transactional
    public PaymentDto recordPayment(Long tenantId, RecordPaymentRequest req, Long receivedBy) {
        return paymentRepository.create(tenantId, req, receivedBy);
    }

    public List<PaymentDto> listPayments(
            Long tenantId,
            Long customerId,
            Long appointmentId,
            OffsetDateTime from,
            OffsetDateTime to,
            int limit
    ) {
        return paymentRepository.listPayments(tenantId, customerId, appointmentId, from, to, limit);
    }

    public Optional<PaymentDto> getPayment(Long tenantId, Long paymentId) {
        return paymentRepository.findById(tenantId, paymentId);
    }

    public CustomerBalanceDto getCustomerBalance(Long tenantId, Long customerId) {
        return paymentRepository.getCustomerBalance(tenantId, customerId);
    }

    public Optional<ReceiptDto> getReceipt(Long tenantId, Long paymentId) {
        Optional<PaymentDto> pOpt = paymentRepository.findById(tenantId, paymentId);
        if (pOpt.isEmpty()) {
            return Optional.empty();
        }
        PaymentDto p = pOpt.get();

        TenantDto tenant = tenantLookupService.findById(tenantId).orElse(null);
        String clinicName = tenant != null ? tenant.name() : "Practice";
        String clinicAddress = "";
        String clinicPhone = "";

        String serviceName = null;
        if (p.appointmentId() != null) {
            var sRec = dsl.select(field(name("s", "name"), String.class))
                    .from(table(name("business", "appointments")).as("a"))
                    .join(table(name("business", "services")).as("s"))
                    .on(field(name("s", "id")).eq(field(name("a", "service_id"))))
                    .where(field(name("a", "id"), Long.class).eq(p.appointmentId()))
                    .fetchOne();
            if (sRec != null) {
                serviceName = sRec.value1();
            }
        }

        return Optional.of(new ReceiptDto(
                p.receiptNo(),
                tenantId,
                clinicName,
                clinicAddress,
                clinicPhone,
                p.customerId(),
                p.customerName(),
                p.customerPhone(),
                p.appointmentId(),
                serviceName,
                p.amountPaise(),
                p.method(),
                p.reference(),
                p.receivedByName(),
                p.receivedAt(),
                p.note()
        ));
    }
}
