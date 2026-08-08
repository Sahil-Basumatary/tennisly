package dev.sahilbasumatary.apigateway.client;

import java.util.List;
import java.util.UUID;

public record ApiKeyValidationResponse(
        UUID organizationId,
        UUID apiKeyId,
        List<String> scopes,
        String planTier,
        String organizationName) {}
