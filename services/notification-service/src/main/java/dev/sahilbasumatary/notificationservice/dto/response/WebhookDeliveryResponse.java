package dev.sahilbasumatary.notificationservice.dto.response;

import dev.sahilbasumatary.notificationservice.entity.DeliveryStatus;
import dev.sahilbasumatary.notificationservice.entity.WebhookDelivery;
import java.time.Instant;
import java.util.UUID;

public record WebhookDeliveryResponse(
        UUID id,
        UUID endpointId,
        UUID organizationId,
        String eventId,
        String eventType,
        DeliveryStatus status,
        int attemptCount,
        int maxAttempts,
        Instant nextAttemptAt,
        Integer lastHttpStatus,
        String lastError,
        Integer responseMs,
        Instant createdAt,
        Instant updatedAt,
        Instant deliveredAt,
        String payload) {

    public static WebhookDeliveryResponse summary(WebhookDelivery delivery) {
        return from(delivery, false);
    }

    public static WebhookDeliveryResponse detail(WebhookDelivery delivery) {
        return from(delivery, true);
    }

    private static WebhookDeliveryResponse from(WebhookDelivery delivery, boolean includePayload) {
        return new WebhookDeliveryResponse(
                delivery.getId(),
                delivery.getEndpointId(),
                delivery.getOrganizationId(),
                delivery.getEventId(),
                delivery.getEventType(),
                delivery.getStatus(),
                delivery.getAttemptCount(),
                delivery.getMaxAttempts(),
                delivery.getNextAttemptAt(),
                delivery.getLastHttpStatus(),
                delivery.getLastError(),
                delivery.getResponseMs(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt(),
                delivery.getDeliveredAt(),
                includePayload ? delivery.getPayload() : null);
    }
}
