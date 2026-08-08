package dev.sahilbasumatary.notificationservice.client.dto;

import java.util.UUID;

public record WebhookSubscription(
        UUID id,
        UUID organizationId,
        String targetUrl,
        String signingSecret) {
}
