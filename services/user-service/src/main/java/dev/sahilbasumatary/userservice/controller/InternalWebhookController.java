package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.userservice.dto.response.WebhookSubscriptionResponse;
import dev.sahilbasumatary.userservice.service.WebhookEndpointService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/webhooks")
public class InternalWebhookController {

    private final WebhookEndpointService webhookService;

    public InternalWebhookController(WebhookEndpointService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<WebhookSubscriptionResponse>> subscriptions(
            @RequestParam String eventType,
            @RequestParam(required = false) UUID organizationId) {
        return ResponseEntity.ok(webhookService.findSubscriptions(eventType, organizationId));
    }

    @GetMapping("/endpoints/{id}")
    public ResponseEntity<WebhookSubscriptionResponse> getEndpoint(@PathVariable UUID id) {
        return ResponseEntity.ok(webhookService.getSubscription(id));
    }

    @PostMapping("/endpoints/{id}/mark-delivered")
    public ResponseEntity<Void> markDelivered(
            @PathVariable UUID id,
            @RequestParam(required = false) Instant deliveredAt) {
        webhookService.markDelivered(id, deliveredAt == null ? Instant.now() : deliveredAt);
        return ResponseEntity.noContent().build();
    }
}
