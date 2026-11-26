package com.booking.business.internal.setup;

import com.booking.business.api.dto.*;
import com.booking.business.internal.customer.CustomerRepository;
import com.booking.business.internal.payment.PaymentRepository;
import com.booking.business.internal.scheduling.AppointmentRepository;
import com.booking.customer.internal.InboundRequestRepository;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.*;

@Service
public class DemoDataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeederService.class);

    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final StaffRepository staffRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final InboundRequestRepository inboundRequestRepository;
    private final DSLContext dsl;

    public DemoDataSeederService(
            CustomerRepository customerRepository,
            ServiceRepository serviceRepository,
            StaffRepository staffRepository,
            WorkingHoursRepository workingHoursRepository,
            AppointmentRepository appointmentRepository,
            PaymentRepository paymentRepository,
            InboundRequestRepository inboundRequestRepository,
            DSLContext dsl
    ) {
        this.customerRepository = customerRepository;
        this.serviceRepository = serviceRepository;
        this.staffRepository = staffRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
        this.inboundRequestRepository = inboundRequestRepository;
        this.dsl = dsl;
    }

    @Transactional
    public void seedRealisticData(Long tenantId) {
        log.info("Starting realistic demo data seeding for tenant {}", tenantId);

        // 1. Ensure Services
        List<ServiceDto> services = serviceRepository.listByTenant(tenantId);
        if (services.isEmpty()) {
            services = List.of(
                    serviceRepository.create(tenantId, "Dental Consultation & X-Ray", 30, 10, 80000), // ₹800
                    serviceRepository.create(tenantId, "Scaling & Polishing", 45, 15, 180000), // ₹1,800
                    serviceRepository.create(tenantId, "Composite Tooth Filling", 45, 10, 220000), // ₹2,200
                    serviceRepository.create(tenantId, "Root Canal Therapy", 60, 15, 650000), // ₹6,500
                    serviceRepository.create(tenantId, "Teeth Whitening", 60, 15, 900000), // ₹9,000
                    serviceRepository.create(tenantId, "Ceramic Crown Fitting", 45, 15, 1200000) // ₹12,000
            );
        }

        List<Long> serviceIds = services.stream().map(ServiceDto::id).toList();

        // 2. Ensure Staff Members
        List<StaffDto> staffList = staffRepository.listByTenant(tenantId);
        StaffDto primaryStaff;
        StaffDto associateStaff;

        if (staffList.isEmpty()) {
            primaryStaff = staffRepository.create(tenantId, "Dr. Priya Sharma", "Lead Dentist", "#A8895A", true, 0, serviceIds);
            associateStaff = staffRepository.create(tenantId, "Dr. Arjun Verma", "Endodontist", "#4A6B82", true, 1, serviceIds);

            // Working hours: Mon - Sat 9:00 - 18:00
            for (StaffDto st : List.of(primaryStaff, associateStaff)) {
                List<WorkingHoursDto.WorkingHoursRequest> hours = new ArrayList<>();
                for (int w = 1; w <= 6; w++) {
                    hours.add(new WorkingHoursDto.WorkingHoursRequest(st.id(), w, LocalTime.of(9, 0), LocalTime.of(18, 0)));
                }
                workingHoursRepository.setStaffWorkingHours(tenantId, st.id(), hours);
            }
        } else {
            primaryStaff = staffList.get(0);
            associateStaff = staffList.size() > 1 ? staffList.get(1) : primaryStaff;
        }

        // 3. Ensure Patients / Customers
        List<CustomerDto> existingCusts = customerRepository.list(tenantId, null, 100);
        List<CustomerDto> customers = new ArrayList<>(existingCusts);

        if (customers.size() < 6) {
            String[][] demoPatients = {
                    {"Rahul Sharma", "+919876543210", "rahul.sharma@gmail.com", "1988-04-12", "Patient prefers morning slots. Mild dental anxiety."},
                    {"Ananya Iyer", "+919812345678", "ananya.iyer@outlook.com", "1994-08-23", "Allergic to penicillin. Regular checkup visitor."},
                    {"Vikram Malhotra", "+919700112233", "vikram.m@techcorp.in", "1982-11-05", "Sensitive upper molars. Interested in cosmetic whitening."},
                    {"Pooja Patel", "+919655443322", "pooja.patel@gmail.com", "1991-02-17", "Referred by Dr. Verma. Completed filling treatment."},
                    {"Amit Deshmukh", "+919922334455", "amit.desh@gmail.com", "1978-06-30", "Diabetic patient. Annual prophylaxis checkup."},
                    {"Sneha Nair", "+919833445566", "sneha.nair@icloud.com", "1996-09-14", "Night guard wearer. Routine scaling completed."},
                    {"Rohan Gupta", "+919744556677", "rohan.gupta@gmail.com", "1985-12-01", "Corporate health plan member."},
                    {"Meera Sundaram", "+919633221100", "meera.sundaram@yahoo.com", "1990-03-25", "Crown fitting follow-up."}
            };

            for (String[] p : demoPatients) {
                var found = customerRepository.findByPhone(tenantId, p[1]);
                if (found.isEmpty()) {
                    customers.add(customerRepository.create(
                            tenantId,
                            p[0],
                            p[1],
                            p[2],
                            LocalDate.parse(p[3]),
                            p[4],
                            "{}"
                    ));
                } else {
                    customers.add(found.get());
                }
            }
        }

        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.of("Asia/Kolkata");

        // 4. Past Completed Appointments + Payments & Follow-ups (Past 3 weeks)
        // Check if appointments already exist to prevent redundant duplicate runs
        Integer apptCount = dsl.selectCount()
                .from(table(name("business", "appointments")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .fetchOne(0, Integer.class);

        if (apptCount == null || apptCount < 5) {
            // Patient 0: Rahul Sharma - Scaling completed 14 days ago
            createCompletedVisitWithPayment(
                    tenantId, customers.get(0), services.get(1), primaryStaff,
                    today.minusDays(14).atTime(10, 0).atZone(zone).toOffsetDateTime(),
                    "Scaling & stain polishing completed. Mild gingivitis resolved.",
                    "happy", "Felt very gentle and no sensitivity afterward.",
                    "upi", "UPI/849201948", "Paid in full via PhonePe"
            );

            // Patient 1: Ananya Iyer - Consultation & X-Ray 10 days ago (Happy, needs follow-up!)
            AppointmentDto apptAnanya = createCompletedVisitWithPayment(
                    tenantId, customers.get(1), services.get(0), associateStaff,
                    today.minusDays(10).atTime(11, 30).atZone(zone).toOffsetDateTime(),
                    "Panoramic X-ray captured. Recommended follow-up in 2 weeks for composite filling.",
                    "happy", "Excellent explanation of the X-ray findings.",
                    "card", "HDFC-POS-9124", "Card tap POS"
            );
            // Mark follow-up needed
            dsl.update(table(name("business", "appointments")))
                    .set(field(name("followup_status"), String.class), "needed")
                    .set(field(name("followup_due_on"), LocalDate.class), today.plusDays(4))
                    .where(field(name("id"), Long.class).eq(apptAnanya.id()))
                    .execute();

            // Patient 2: Vikram Malhotra - Tooth Filling 7 days ago
            createCompletedVisitWithPayment(
                    tenantId, customers.get(2), services.get(2), primaryStaff,
                    today.minusDays(7).atTime(15, 0).atZone(zone).toOffsetDateTime(),
                    "Composite restoration on tooth #26. Bite adjusted and verified.",
                    "happy", "Very smooth finish, bite feels completely natural.",
                    "upi", "UPI/902814811", "GooglePay payment"
            );

            // Patient 3: Pooja Patel - Root Canal 4 days ago (Large procedure, partial payment)
            AppointmentDto apptPooja = createCompletedVisit(
                    tenantId, customers.get(3), services.get(3), associateStaff,
                    today.minusDays(4).atTime(14, 0).atZone(zone).toOffsetDateTime(),
                    "First stage RCT completed. Canal obturated, temporary dressing placed.",
                    "happy", "No pain during the procedure. Prescribed post-op analgesics."
            );
            // Partial payment of ₹4,000 out of ₹6,500
            paymentRepository.create(
                    tenantId,
                    new RecordPaymentRequest(customers.get(3).id(), apptPooja.id(), 400000, "upi", "UPI/109284719", "Partial payment for RCT stage 1"),
                    null
            );

            // Patient 4: Amit Deshmukh - Scaling & Polishing 3 days ago (Happy patient for follow-up call desk!)
            AppointmentDto apptAmit = createCompletedVisitWithPayment(
                    tenantId, customers.get(4), services.get(1), primaryStaff,
                    today.minusDays(3).atTime(16, 0).atZone(zone).toOffsetDateTime(),
                    "Supragingival plaque removed. Patient recommended for sensitivity rinse.",
                    "happy", "Appreciated the promptness and clean equipment.",
                    "cash", null, "Cash payment at counter"
            );
            dsl.update(table(name("business", "appointments")))
                    .set(field(name("followup_status"), String.class), "none")
                    .where(field(name("id"), Long.class).eq(apptAmit.id()))
                    .execute();

            // 5. Today's Live Schedule Stream
            // A. Morning Completed (9:30 AM)
            createCompletedVisitWithPayment(
                    tenantId, customers.get(5), services.get(0), primaryStaff,
                    today.atTime(9, 30).atZone(zone).toOffsetDateTime(),
                    "Routine oral examination. Healthy gingiva.",
                    "happy", "Great experience.",
                    "upi", "UPI/772819401", "Morning visit payment"
            );

            // B. Currently In-Progress (11:00 AM)
            AppointmentDto inProgressAppt = appointmentRepository.create(
                    tenantId, customers.get(6).id(), services.get(2).id(), associateStaff.id(),
                    today.atTime(11, 0).atZone(zone).toOffsetDateTime(),
                    today.atTime(11, 45).atZone(zone).toOffsetDateTime(),
                    "phone", "Restoration on lower right premolar",
                    services.get(2).pricePaise(), null
            );
            appointmentRepository.updateStatus(tenantId, inProgressAppt.id(), "in_progress", "Started filling", null, null, null, null, null, inProgressAppt.version());

            // C. Checked In / Arrived in Waiting Room (12:00 PM)
            AppointmentDto arrivedAppt = appointmentRepository.create(
                    tenantId, customers.get(7).id(), services.get(1).id(), primaryStaff.id(),
                    today.atTime(12, 0).atZone(zone).toOffsetDateTime(),
                    today.atTime(12, 45).atZone(zone).toOffsetDateTime(),
                    "web", "Routine cleaning",
                    services.get(1).pricePaise(), null
            );
            appointmentRepository.updateStatus(tenantId, arrivedAppt.id(), "arrived", "Patient seated in reception", null, null, null, null, null, arrivedAppt.version());

            // D. Confirmed Afternoon Bookings
            appointmentRepository.create(
                    tenantId, customers.get(0).id(), services.get(0).id(), primaryStaff.id(),
                    today.atTime(15, 0).atZone(zone).toOffsetDateTime(),
                    today.atTime(15, 30).atZone(zone).toOffsetDateTime(),
                    "phone", "Regular 6-month checkup",
                    services.get(0).pricePaise(), null
            );

            appointmentRepository.create(
                    tenantId, customers.get(2).id(), services.get(4).id(), associateStaff.id(),
                    today.atTime(16, 30).atZone(zone).toOffsetDateTime(),
                    today.atTime(17, 30).atZone(zone).toOffsetDateTime(),
                    "phone", "In-office teeth whitening session",
                    services.get(4).pricePaise(), null
            );

            // E. Future Bookings (Tomorrow & next week)
            appointmentRepository.create(
                    tenantId, customers.get(3).id(), services.get(3).id(), associateStaff.id(),
                    today.plusDays(2).atTime(10, 0).atZone(zone).toOffsetDateTime(),
                    today.plusDays(2).atTime(11, 0).atZone(zone).toOffsetDateTime(),
                    "phone", "RCT Stage 2 crown prep",
                    services.get(3).pricePaise(), null
            );

            appointmentRepository.create(
                    tenantId, customers.get(1).id(), services.get(2).id(), primaryStaff.id(),
                    today.plusDays(4).atTime(11, 30).atZone(zone).toOffsetDateTime(),
                    today.plusDays(4).atTime(12, 15).atZone(zone).toOffsetDateTime(),
                    "phone", "Follow-up filling as advised in X-ray",
                    services.get(2).pricePaise(), null
            );
        }

        // 6. Seed Inbound Web Requests into customer.inbound_requests
        Integer inboundCount = dsl.selectCount()
                .from(table(name("customer", "inbound_requests")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .fetchOne(0, Integer.class);

        if (inboundCount == null || inboundCount < 2) {
            inboundRequestRepository.create(
                    tenantId,
                    new SubmitInboundRequest(
                            "appointment",
                            "Kavita Krishnan",
                            "+919845012345",
                            "kavita.k@gmail.com",
                            services.get(1).id(),
                            today.plusDays(1),
                            "morning",
                            "Need urgent dental cleaning before a family wedding this weekend.",
                            null
                    ),
                    "+919845012345"
            );

            inboundRequestRepository.create(
                    tenantId,
                    new SubmitInboundRequest(
                            "call_back",
                            "Suresh Nambiar",
                            "+919876098760",
                            "suresh.nambiar@yahoo.com",
                            null,
                            null,
                            null,
                            "Looking for information on dental implants and EMI payment options.",
                            null
                    ),
                    "+919876098760"
            );
        }

        // 7. Seed Waitlist entries if empty
        Integer waitlistCount = dsl.selectCount()
                .from(table(name("business", "waitlist_entries")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .fetchOne(0, Integer.class);

        if (waitlistCount == null || waitlistCount == 0) {
            dsl.insertInto(table(name("business", "waitlist_entries")))
                    .columns(
                            field(name("tenant_id"), Long.class),
                            field(name("customer_id"), Long.class),
                            field(name("service_id"), Long.class),
                            field(name("staff_id"), Long.class),
                            field(name("earliest_date"), LocalDate.class),
                            field(name("latest_date"), LocalDate.class),
                            field(name("notes"), String.class),
                            field(name("status"), String.class)
                    )
                    .values(
                            tenantId,
                            customers.get(0).id(),
                            services.get(4).id(),
                            primaryStaff.id(),
                            today,
                            today.plusDays(7),
                            "Wants an earlier slot for teeth whitening if any cancellation opens up.",
                            "waiting"
                    )
                    .execute();
        }

        log.info("Realistic demo data seeding completed successfully for tenant {}", tenantId);
    }

    private AppointmentDto createCompletedVisit(
            Long tenantId,
            CustomerDto customer,
            ServiceDto service,
            StaffDto staff,
            OffsetDateTime startsAt,
            String outcome,
            String reactionNote
    ) {
        OffsetDateTime endsAt = startsAt.plusMinutes(service.durationMin());
        AppointmentDto appt = appointmentRepository.create(
                tenantId,
                customer.id(),
                service.id(),
                staff.id(),
                startsAt,
                endsAt,
                "phone",
                "Regular appointment",
                service.pricePaise(),
                null
        );

        return appointmentRepository.updateStatus(
                tenantId,
                appt.id(),
                "completed",
                "Completed on schedule",
                outcome,
                "happy",
                reactionNote,
                null,
                null,
                appt.version()
        );
    }

    private AppointmentDto createCompletedVisitWithPayment(
            Long tenantId,
            CustomerDto customer,
            ServiceDto service,
            StaffDto staff,
            OffsetDateTime startsAt,
            String outcome,
            String reaction,
            String reactionNote,
            String paymentMethod,
            String paymentRef,
            String paymentNote
    ) {
        OffsetDateTime endsAt = startsAt.plusMinutes(service.durationMin());
        AppointmentDto appt = appointmentRepository.create(
                tenantId,
                customer.id(),
                service.id(),
                staff.id(),
                startsAt,
                endsAt,
                "phone",
                "Regular appointment",
                service.pricePaise(),
                null
        );

        AppointmentDto completed = appointmentRepository.updateStatus(
                tenantId,
                appt.id(),
                "completed",
                "Completed on schedule",
                outcome,
                reaction,
                reactionNote,
                null,
                null,
                appt.version()
        );

        paymentRepository.create(
                tenantId,
                new RecordPaymentRequest(
                        customer.id(),
                        completed.id(),
                        completed.pricePaise(),
                        paymentMethod,
                        paymentRef,
                        paymentNote
                ),
                null
        );

        return completed;
    }
}
