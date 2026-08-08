package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.userservice.dto.request.AdminUpdateUserRequest;
import dev.sahilbasumatary.userservice.dto.response.AdminPageResponse;
import dev.sahilbasumatary.userservice.dto.response.AdminUserResponse;
import dev.sahilbasumatary.userservice.service.AdminUserService;
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
@RequestMapping("/api/users/admin/users")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<AdminPageResponse<AdminUserResponse>> listUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/users/admin/users q={} active={}", q, active);
        return ResponseEntity.ok(adminUserService.list(q, active, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable UUID id) {
        log.debug("GET /api/users/admin/users/{}", id);
        return ResponseEntity.ok(adminUserService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable UUID id, @Valid @RequestBody AdminUpdateUserRequest request) {
        log.debug("PUT /api/users/admin/users/{}", id);
        return ResponseEntity.ok(adminUserService.update(id, request));
    }
}
