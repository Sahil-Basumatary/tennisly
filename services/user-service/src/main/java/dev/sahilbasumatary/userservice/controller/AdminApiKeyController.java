package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.userservice.dto.request.AdminCreateApiKeyRequest;
import dev.sahilbasumatary.userservice.dto.response.AdminApiKeyResponse;
import dev.sahilbasumatary.userservice.dto.response.AdminCreateApiKeyResponse;
import dev.sahilbasumatary.userservice.dto.response.AdminPageResponse;
import dev.sahilbasumatary.userservice.service.ApiKeyService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/admin/api-keys")
public class AdminApiKeyController {

    private static final Logger log = LoggerFactory.getLogger(AdminApiKeyController.class);
    private final ApiKeyService apiKeyService;

    public AdminApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public ResponseEntity<AdminPageResponse<AdminApiKeyResponse>> listApiKeys(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/users/admin/api-keys organizationId={} active={}", organizationId, active);
        return ResponseEntity.ok(apiKeyService.list(organizationId, active, page, size));
    }

    @PostMapping
    public ResponseEntity<AdminCreateApiKeyResponse> createApiKey(
            @Valid @RequestBody AdminCreateApiKeyRequest request) {
        log.debug("POST /api/users/admin/api-keys organizationId={}", request.organizationId());
        return ResponseEntity.ok(apiKeyService.create(request));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<AdminApiKeyResponse> revokeApiKey(@PathVariable UUID id) {
        log.debug("POST /api/users/admin/api-keys/{}/revoke", id);
        return ResponseEntity.ok(apiKeyService.revoke(id));
    }
}
