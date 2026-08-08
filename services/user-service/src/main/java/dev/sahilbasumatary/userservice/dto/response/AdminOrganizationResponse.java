package dev.sahilbasumatary.userservice.dto.response;

import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.PlanTier;
import java.time.Instant;
import java.util.UUID;

public record AdminOrganizationResponse(
        UUID id,
        String clerkOrgId,
        String name,
        String slug,
        String description,
        String logoUrl,
        String website,
        PlanTier planTier,
        int maxMembers,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminOrganizationResponse from(Organization org) {
        return new AdminOrganizationResponse(
                org.getId(),
                org.getClerkOrgId(),
                org.getName(),
                org.getSlug(),
                org.getDescription(),
                org.getLogoUrl(),
                org.getWebsite(),
                org.getPlanTier(),
                org.getMaxMembers(),
                org.isActive(),
                org.getCreatedAt(),
                org.getUpdatedAt());
    }
}
