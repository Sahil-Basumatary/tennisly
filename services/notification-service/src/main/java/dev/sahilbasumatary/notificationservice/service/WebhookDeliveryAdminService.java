package dev.sahilbasumatary.notificationservice.service;

import dev.sahilbasumatary.notificationservice.dto.response.AdminPageResponse;
import dev.sahilbasumatary.notificationservice.dto.response.WebhookDeliveryResponse;
import dev.sahilbasumatary.notificationservice.entity.DeliveryStatus;
import dev.sahilbasumatary.notificationservice.entity.WebhookDelivery;
import dev.sahilbasumatary.notificationservice.entity.WebhookDeliveryRepository;
import dev.sahilbasumatary.notificationservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.notificationservice.security.AdminAccess;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookDeliveryAdminService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryAdminService.class);

    private final WebhookDeliveryRepository deliveryRepository;

    public WebhookDeliveryAdminService(WebhookDeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<WebhookDeliveryResponse> list(
            UUID organizationId,
            UUID endpointId,
            DeliveryStatus status,
            String eventType,
            int page,
            int size) {
        AdminAccess.assertPlatformAdmin();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String normalizedEventType =
                eventType == null || eventType.isBlank() ? null : eventType.trim();
        Page<WebhookDeliveryResponse> result =
                deliveryRepository
                        .search(organizationId, endpointId, status, normalizedEventType, pageable)
                        .map(WebhookDeliveryResponse::summary);
        return AdminPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public WebhookDeliveryResponse get(UUID id) {
        AdminAccess.assertPlatformAdmin();
        WebhookDelivery delivery =
                deliveryRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("WebhookDelivery", id));
        return WebhookDeliveryResponse.detail(delivery);
    }

    @Transactional
    public WebhookDeliveryResponse retry(UUID id) {
        AdminAccess.assertPlatformAdmin();
        WebhookDelivery delivery =
                deliveryRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("WebhookDelivery", id));
        if (delivery.getStatus() == DeliveryStatus.SUCCESS) {
            throw new IllegalStateException("Successful deliveries cannot be retried");
        }
        DeliveryStatus previous = delivery.getStatus();
        // Manual requeue: DEAD rows get a fresh attempt budget; FAILED keeps history but runs now.
        if (previous == DeliveryStatus.DEAD) {
            delivery.setAttemptCount(0);
        }
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setNextAttemptAt(Instant.now());
        delivery.setLastError(null);
        WebhookDelivery saved = deliveryRepository.save(delivery);
        log.info(
                "Admin requeued webhook delivery id={} previousStatus={} attemptCount={}",
                id,
                previous,
                saved.getAttemptCount());
        return WebhookDeliveryResponse.summary(saved);
    }
}
