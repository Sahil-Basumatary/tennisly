package dev.sahilbasumatary.userservice.dto.response;

import dev.sahilbasumatary.userservice.entity.AuditLog;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AdminAuditLogResponse(
        UUID id,
        String actorClerkId,
        String actorEmail,
        String action,
        String resourceType,
        String resourceId,
        UUID organizationId,
        Map<String, Object> metadata,
        String ipAddress,
        Instant createdAt) {

    public static AdminAuditLogResponse from(AuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getActorClerkId(),
                log.getActorEmail(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getOrganizationId(),
                log.getMetadata(),
                log.getIpAddress(),
                log.getCreatedAt());
    }
}
