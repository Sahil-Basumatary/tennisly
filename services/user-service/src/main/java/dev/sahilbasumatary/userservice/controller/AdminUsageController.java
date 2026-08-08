package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.userservice.dto.response.AdminUsageResponse;
import dev.sahilbasumatary.userservice.service.UsageMeter;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/admin/usage")
public class AdminUsageController {

    private static final Logger log = LoggerFactory.getLogger(AdminUsageController.class);
    private final UsageMeter usageMeter;

    public AdminUsageController(UsageMeter usageMeter) {
        this.usageMeter = usageMeter;
    }

    @GetMapping
    public ResponseEntity<AdminUsageResponse> getUsage(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.debug("GET /api/users/admin/usage organizationId={}", organizationId);
        return ResponseEntity.ok(usageMeter.list(organizationId, from, to));
    }
}
