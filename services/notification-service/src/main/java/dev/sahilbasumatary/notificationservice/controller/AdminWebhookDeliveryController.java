package dev.sahilbasumatary.notificationservice.controller;

import dev.sahilbasumatary.notificationservice.dto.response.AdminPageResponse;
import dev.sahilbasumatary.notificationservice.dto.response.WebhookDeliveryResponse;
import dev.sahilbasumatary.notificationservice.entity.DeliveryStatus;
import dev.sahilbasumatary.notificationservice.service.WebhookDeliveryAdminService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/admin/deliveries")
public class AdminWebhookDeliveryController {

    private static final Logger log = LoggerFactory.getLogger(AdminWebhookDeliveryController.class);

    private final WebhookDeliveryAdminService deliveryAdminService;

    public AdminWebhookDeliveryController(WebhookDeliveryAdminService deliveryAdminService) {
        this.deliveryAdminService = deliveryAdminService;
    }

    @GetMapping
    public ResponseEntity<AdminPageResponse<WebhookDeliveryResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID endpointId,
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        log.debug(
                "GET /api/notifications/admin/deliveries orgId={} endpointId={} status={}",
                organizationId,
                endpointId,
                status);
        return ResponseEntity.ok(
                deliveryAdminService.list(organizationId, endpointId, status, eventType, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookDeliveryResponse> get(@PathVariable UUID id) {
        log.debug("GET /api/notifications/admin/deliveries/{}", id);
        return ResponseEntity.ok(deliveryAdminService.get(id));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<WebhookDeliveryResponse> retry(@PathVariable UUID id) {
        log.debug("POST /api/notifications/admin/deliveries/{}/retry", id);
        return ResponseEntity.ok(deliveryAdminService.retry(id));
    }
}
