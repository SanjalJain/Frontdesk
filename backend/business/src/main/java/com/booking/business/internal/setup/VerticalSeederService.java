package com.booking.business.internal.setup;

import com.booking.business.api.dto.ServiceDto;
import com.booking.business.api.dto.StaffDto;
import com.booking.business.api.dto.WorkingHoursDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VerticalSeederService {

    private static final Logger log = LoggerFactory.getLogger(VerticalSeederService.class);

    private final ServiceRepository serviceRepository;
    private final StaffRepository staffRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final ObjectMapper objectMapper;

    public VerticalSeederService(
            ServiceRepository serviceRepository,
            StaffRepository staffRepository,
            WorkingHoursRepository workingHoursRepository,
            ObjectMapper objectMapper
    ) {
        this.serviceRepository = serviceRepository;
        this.staffRepository = staffRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<ServiceDto> seedFromTemplate(Long tenantId, String vertical, String primaryStaffName) {
        String templateName = (vertical != null && !vertical.isBlank())
                ? vertical.trim().toLowerCase()
                : "dental";

        List<ServiceDto> seededServices = new ArrayList<>();
        try {
            var resource = new ClassPathResource("templates/verticals/" + templateName + ".json");
            if (!resource.exists()) {
                log.warn("Template {}.json not found, falling back to generic", templateName);
                resource = new ClassPathResource("templates/verticals/generic.json");
            }

            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                JsonNode defaultServices = root.get("default_services");
                if (defaultServices != null && defaultServices.isArray()) {
                    for (JsonNode s : defaultServices) {
                        String name = s.get("name").asText();
                        int duration = s.get("duration_min").asInt(30);
                        int price = s.get("price_paise").asInt(0);

                        ServiceDto created = serviceRepository.create(tenantId, name, duration, 0, price);
                        seededServices.add(created);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to seed template services for tenant {}: {}", tenantId, e.getMessage(), e);
        }

        // If no staff exists, seed primary staff member with default 9:00 - 18:00 weekday hours
        List<StaffDto> existingStaff = staffRepository.listByTenant(tenantId);
        if (existingStaff.isEmpty()) {
            List<Long> serviceIds = seededServices.stream().map(ServiceDto::id).toList();
            String staffName = (primaryStaffName != null && !primaryStaffName.isBlank())
                    ? primaryStaffName.trim()
                    : "Primary Practitioner";

            StaffDto staff = staffRepository.create(
                    tenantId,
                    staffName,
                    "Lead",
                    "#A8895A",
                    true,
                    0,
                    serviceIds
            );

            // Seed Monday through Saturday 9:00 - 18:00
            List<WorkingHoursDto.WorkingHoursRequest> hours = new ArrayList<>();
            for (int weekday = 1; weekday <= 6; weekday++) {
                hours.add(new WorkingHoursDto.WorkingHoursRequest(
                        staff.id(),
                        weekday,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0)
                ));
            }
            workingHoursRepository.setStaffWorkingHours(tenantId, staff.id(), hours);
        }

        return seededServices;
    }
}
