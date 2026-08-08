package dev.sahilbasumatary.notificationservice.service;

import dev.sahilbasumatary.common.event.BaseEvent;
import dev.sahilbasumatary.notificationservice.client.UserServiceWebhookClient;
import dev.sahilbasumatary.notificationservice.client.dto.WebhookSubscription;
import dev.sahilbasumatary.notificationservice.entity.DeliveryStatus;
import dev.sahilbasumatary.notificationservice.entity.WebhookDelivery;
import dev.sahilbasumatary.notificationservice.entity.WebhookDeliveryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EnqueueService {

    private static final Logger log = LoggerFactory.getLogger(EnqueueService.class);

    private final UserServiceWebhookClient webhookClient;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookPayloadBuilder payloadBuilder;

    public EnqueueService(
            UserServiceWebhookClient webhookClient,
            WebhookDeliveryRepository deliveryRepository,
            WebhookPayloadBuilder payloadBuilder) {
        this.webhookClient = webhookClient;
        this.deliveryRepository = deliveryRepository;
        this.payloadBuilder = payloadBuilder;
    }

    public void enqueue(String webhookEventType, BaseEvent sourceEvent) {
        enqueue(webhookEventType, sourceEvent, null);
    }

    public void enqueue(String webhookEventType, BaseEvent sourceEvent, UUID organizationId) {
        List<WebhookSubscription> subscriptions =
                webhookClient.getSubscriptions(webhookEventType, organizationId);
        if (subscriptions == null || subscriptions.isEmpty()) {
            return;
        }
        String payload = payloadBuilder.buildEnvelope(webhookEventType, sourceEvent);
        for (WebhookSubscription sub : subscriptions) {
            String idempotencyKey = sub.id() + ":" + sourceEvent.getEventId();
            if (deliveryRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.debug("Skipping duplicate delivery idempotencyKey={}", idempotencyKey);
                continue;
            }
            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setEndpointId(sub.id());
            delivery.setOrganizationId(sub.organizationId());
            delivery.setEventId(sourceEvent.getEventId());
            delivery.setEventType(webhookEventType);
            delivery.setPayload(payload);
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setNextAttemptAt(Instant.now());
            delivery.setIdempotencyKey(idempotencyKey);
            deliveryRepository.save(delivery);
            log.info("Enqueued webhook delivery endpoint={} eventType={} eventId={}",
                    sub.id(), webhookEventType, sourceEvent.getEventId());
        }
    }
}
