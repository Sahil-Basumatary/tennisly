package dev.sahilbasumatary.userservice.dto.response;

import dev.sahilbasumatary.userservice.entity.OrganizationWebhookEndpoint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WebhookEndpointResponse(
        UUID id,
        UUID organizationId,
        String name,
        String targetUrl,
        String secretPrefix,
        List<String> eventTypes,
        boolean active,
        String description,
        String createdByClerkId,
        Instant revokedAt,
        Instant lastDeliveryAt,
        Instant createdAt,
        Instant updatedAt) {

    public static WebhookEndpointResponse from(OrganizationWebhookEndpoint endpoint) {
        return new WebhookEndpointResponse(
                endpoint.getId(),
                endpoint.getOrganization().getId(),
                endpoint.getName(),
                endpoint.getTargetUrl(),
                endpoint.getSecretPrefix(),
                endpoint.getEventTypes(),
                endpoint.isActive(),
                endpoint.getDescription(),
                endpoint.getCreatedByClerkId(),
                endpoint.getRevokedAt(),
                endpoint.getLastDeliveryAt(),
                endpoint.getCreatedAt(),
                endpoint.getUpdatedAt());
    }
}
