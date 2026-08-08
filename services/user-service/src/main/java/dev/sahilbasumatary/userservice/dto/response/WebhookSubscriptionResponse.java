package dev.sahilbasumatary.userservice.dto.response;

import java.util.List;
import java.util.UUID;

public record WebhookSubscriptionResponse(
        UUID id,
        UUID organizationId,
        String targetUrl,
        String signingSecret,
        List<String> eventTypes) {}
