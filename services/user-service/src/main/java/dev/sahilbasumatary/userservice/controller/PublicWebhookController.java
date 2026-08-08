package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.dto.request.CreateWebhookEndpointRequest;
import dev.sahilbasumatary.userservice.dto.response.CreateWebhookEndpointResponse;
import dev.sahilbasumatary.userservice.dto.response.WebhookEndpointResponse;
import dev.sahilbasumatary.userservice.exception.UnauthorizedAccessException;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/public/webhooks")
public class PublicWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PublicWebhookController.class);
    private final WebhookEndpointService webhookService;

    public PublicWebhookController(WebhookEndpointService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    public ResponseEntity<List<WebhookEndpointResponse>> list() {
        UUID orgId = requireOrgId();
        log.debug("GET /api/users/public/webhooks orgId={}", orgId);
        return ResponseEntity.ok(webhookService.list(orgId));
    }

    @PostMapping
    public ResponseEntity<CreateWebhookEndpointResponse> create(
            @Valid @RequestBody CreateWebhookEndpointRequest request) {
        UUID orgId = requireOrgId();
        log.debug("POST /api/users/public/webhooks orgId={}", orgId);
        return ResponseEntity.ok(webhookService.create(request, orgId));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<WebhookEndpointResponse> revoke(@PathVariable UUID id) {
        UUID orgId = requireOrgId();
        log.debug("POST /api/users/public/webhooks/{}/revoke", id);
        return ResponseEntity.ok(webhookService.revoke(id, orgId));
    }

    @PostMapping("/{id}/rotate-secret")
    public ResponseEntity<CreateWebhookEndpointResponse> rotateSecret(@PathVariable UUID id) {
        UUID orgId = requireOrgId();
        log.debug("POST /api/users/public/webhooks/{}/rotate-secret", id);
        return ResponseEntity.ok(webhookService.rotateSecret(id, orgId));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Void> test(@PathVariable UUID id) {
        UUID orgId = requireOrgId();
        log.debug("POST /api/users/public/webhooks/{}/test", id);
        webhookService.test(id, orgId);
        return ResponseEntity.accepted().build();
    }

    private UUID requireOrgId() {
        String orgIdStr = RequestContext.getOrgId();
        if (orgIdStr == null || orgIdStr.isBlank()) {
            throw new UnauthorizedAccessException("X-Org-Id header required for webhook operations");
        }
        return UUID.fromString(orgIdStr);
    }
}
