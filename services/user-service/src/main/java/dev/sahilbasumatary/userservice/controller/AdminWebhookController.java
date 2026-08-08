package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.userservice.dto.request.CreateWebhookEndpointRequest;
import dev.sahilbasumatary.userservice.dto.response.CreateWebhookEndpointResponse;
import dev.sahilbasumatary.userservice.dto.response.WebhookEndpointResponse;
import dev.sahilbasumatary.userservice.security.AdminAccess;
import dev.sahilbasumatary.userservice.service.WebhookEndpointService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/users/admin/webhooks")
public class AdminWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AdminWebhookController.class);
    private final WebhookEndpointService webhookService;

    public AdminWebhookController(WebhookEndpointService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    public ResponseEntity<List<WebhookEndpointResponse>> list(
            @RequestParam UUID organizationId) {
        AdminAccess.assertPlatformAdmin();
        log.debug("GET /api/users/admin/webhooks orgId={}", organizationId);
        return ResponseEntity.ok(webhookService.list(organizationId));
    }

    @PostMapping
    public ResponseEntity<CreateWebhookEndpointResponse> create(
            @Valid @RequestBody CreateWebhookEndpointRequest request) {
        AdminAccess.assertPlatformAdmin();
        log.debug("POST /api/users/admin/webhooks orgId={}", request.organizationId());
        if (request.organizationId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(webhookService.create(request, request.organizationId()));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<WebhookEndpointResponse> revoke(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        AdminAccess.assertPlatformAdmin();
        log.debug("POST /api/users/admin/webhooks/{}/revoke", id);
        return ResponseEntity.ok(webhookService.revoke(id, organizationId));
    }

    @PostMapping("/{id}/rotate-secret")
    public ResponseEntity<CreateWebhookEndpointResponse> rotateSecret(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        AdminAccess.assertPlatformAdmin();
        log.debug("POST /api/users/admin/webhooks/{}/rotate-secret", id);
        return ResponseEntity.ok(webhookService.rotateSecret(id, organizationId));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Void> test(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        AdminAccess.assertPlatformAdmin();
        log.debug("POST /api/users/admin/webhooks/{}/test", id);
        webhookService.test(id, organizationId);
        return ResponseEntity.accepted().build();
    }
}
