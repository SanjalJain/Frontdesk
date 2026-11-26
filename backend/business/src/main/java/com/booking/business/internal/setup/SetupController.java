package com.booking.business.internal.setup;

import com.booking.business.api.dto.ServiceDto;
import com.booking.business.api.dto.StaffDto;
import com.booking.business.api.dto.TimeOffDto;
import com.booking.business.api.dto.WorkingHoursDto;
import com.booking.core.api.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/biz/setup")
public class SetupController {

    private final ServiceRepository serviceRepository;
    private final StaffRepository staffRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final TimeOffRepository timeOffRepository;
    private final VerticalSeederService verticalSeederService;
    private final DemoDataSeederService demoDataSeederService;

    public SetupController(
            ServiceRepository serviceRepository,
            StaffRepository staffRepository,
            WorkingHoursRepository workingHoursRepository,
            TimeOffRepository timeOffRepository,
            VerticalSeederService verticalSeederService,
            DemoDataSeederService demoDataSeederService
    ) {
        this.serviceRepository = serviceRepository;
        this.staffRepository = staffRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.timeOffRepository = timeOffRepository;
        this.verticalSeederService = verticalSeederService;
        this.demoDataSeederService = demoDataSeederService;
    }

    @PostMapping("/onboarding/seed")
    public ResponseEntity<List<ServiceDto>> seedOnboarding(
            @RequestParam(name = "vertical") String vertical,
            @RequestParam(name = "primaryStaffName", required = false) String primaryStaffName
    ) {
        Long tenantId = TenantContext.requireTenantId();
        List<ServiceDto> seeded = verticalSeederService.seedFromTemplate(tenantId, vertical, primaryStaffName);
        return ResponseEntity.ok(seeded);
    }

    @PostMapping("/demo-seed")
    public ResponseEntity<java.util.Map<String, String>> seedDemoData() {
        Long tenantId = TenantContext.requireTenantId();
        demoDataSeederService.seedRealisticData(tenantId);
        return ResponseEntity.ok(java.util.Map.of("message", "Demo data seeded successfully"));
    }

    // --- Services ---
    @GetMapping("/services")
    public ResponseEntity<List<ServiceDto>> listServices() {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(serviceRepository.listByTenant(tenantId));
    }

    @PostMapping("/services")
    public ResponseEntity<ServiceDto> createService(@Valid @RequestBody ServiceDto.CreateServiceRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ServiceDto created = serviceRepository.create(
                tenantId,
                request.name(),
                request.durationMin(),
                request.bufferAfterMin(),
                request.pricePaise()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // --- Staff ---
    @GetMapping("/staff")
    public ResponseEntity<List<StaffDto>> listStaff() {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(staffRepository.listByTenant(tenantId));
    }

    @PostMapping("/staff")
    public ResponseEntity<StaffDto> createStaff(@Valid @RequestBody StaffDto.CreateStaffRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        StaffDto created = staffRepository.create(
                tenantId,
                request.name(),
                request.title(),
                request.color(),
                request.isBookable() != null ? request.isBookable() : true,
                request.sortOrder() != null ? request.sortOrder() : 0,
                request.serviceIds()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // --- Working Hours ---
    @GetMapping("/hours")
    public ResponseEntity<List<WorkingHoursDto>> listHours() {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(workingHoursRepository.listByTenant(tenantId));
    }

    @PutMapping("/hours/staff/{staffId}")
    public ResponseEntity<Void> setStaffHours(
            @PathVariable(name = "staffId") Long staffId,
            @RequestBody List<WorkingHoursDto.WorkingHoursRequest> hours
    ) {
        Long tenantId = TenantContext.requireTenantId();
        workingHoursRepository.setStaffWorkingHours(tenantId, staffId, hours);
        return ResponseEntity.noContent().build();
    }

    // --- Time Off ---
    @GetMapping("/time-off")
    public ResponseEntity<List<TimeOffDto>> listTimeOff() {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(timeOffRepository.listByTenant(tenantId));
    }

    @PostMapping("/time-off")
    public ResponseEntity<TimeOffDto> createTimeOff(@Valid @RequestBody TimeOffDto.CreateTimeOffRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        TimeOffDto created = timeOffRepository.create(
                tenantId,
                request.staffId(),
                request.startsAt(),
                request.endsAt(),
                request.reason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/time-off/{id}")
    public ResponseEntity<Void> deleteTimeOff(@PathVariable(name = "id") Long id) {
        Long tenantId = TenantContext.requireTenantId();
        timeOffRepository.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // --- Logo Upload ---
    @PostMapping("/logo")
    public ResponseEntity<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) {
        Long tenantId = TenantContext.requireTenantId();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            Path uploadDir = Paths.get("backend/business/src/main/resources/static/uploads/logos");
            Files.createDirectories(uploadDir);

            String originalName = file.getOriginalFilename();
            String ext = (originalName != null && originalName.contains("."))
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : ".png";

            String filename = "tenant_" + tenantId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String publicPath = "/static/uploads/logos/" + filename;
            return ResponseEntity.ok(Map.of("logoPath", publicPath));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to store uploaded logo: " + e.getMessage()));
        }
    }
}
