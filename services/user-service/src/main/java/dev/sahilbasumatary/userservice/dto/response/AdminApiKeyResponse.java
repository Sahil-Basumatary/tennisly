package dev.sahilbasumatary.userservice.dto.response;

import dev.sahilbasumatary.userservice.entity.OrganizationApiKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminApiKeyResponse(
        UUID id,
        UUID organizationId,
        String name,
        String keyPrefix,
        List<String> scopes,
        boolean active,
        Instant lastUsedAt,
        Instant expiresAt,
        String createdByClerkId,
        Instant revokedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminApiKeyResponse from(OrganizationApiKey key) {
        return new AdminApiKeyResponse(
                key.getId(),
                key.getOrganization().getId(),
                key.getName(),
                key.getKeyPrefix(),
                key.getScopes(),
                key.isActive(),
                key.getLastUsedAt(),
                key.getExpiresAt(),
                key.getCreatedByClerkId(),
                key.getRevokedAt(),
                key.getCreatedAt(),
                key.getUpdatedAt());
    }
}
