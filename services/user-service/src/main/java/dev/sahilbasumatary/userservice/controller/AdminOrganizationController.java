package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.userservice.dto.request.AdminUpdateOrganizationRequest;
import dev.sahilbasumatary.userservice.dto.response.AdminOrganizationResponse;
import dev.sahilbasumatary.userservice.dto.response.AdminPageResponse;
import dev.sahilbasumatary.userservice.service.AdminOrganizationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/admin/organizations")
public class AdminOrganizationController {

    private static final Logger log = LoggerFactory.getLogger(AdminOrganizationController.class);
    private final AdminOrganizationService adminOrganizationService;

    public AdminOrganizationController(AdminOrganizationService adminOrganizationService) {
        this.adminOrganizationService = adminOrganizationService;
    }

    @GetMapping
    public ResponseEntity<AdminPageResponse<AdminOrganizationResponse>> listOrganizations(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/users/admin/organizations q={} active={}", q, active);
        return ResponseEntity.ok(adminOrganizationService.list(q, active, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminOrganizationResponse> getOrganization(@PathVariable UUID id) {
        log.debug("GET /api/users/admin/organizations/{}", id);
        return ResponseEntity.ok(adminOrganizationService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminOrganizationResponse> updateOrganization(
            @PathVariable UUID id, @Valid @RequestBody AdminUpdateOrganizationRequest request) {
        log.debug("PUT /api/users/admin/organizations/{}", id);
        return ResponseEntity.ok(adminOrganizationService.update(id, request));
    }
}
