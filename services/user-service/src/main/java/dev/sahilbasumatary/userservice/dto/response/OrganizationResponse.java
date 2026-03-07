package dev.sahilbasumatary.userservice.dto.response;

import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.PlanTier;
import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String logoUrl,
        String website,
        PlanTier planTier,
        int maxMembers,
        Instant createdAt,
        Instant updatedAt) {

    public static OrganizationResponse from(Organization org) {
        return new OrganizationResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getDescription(),
                org.getLogoUrl(),
                org.getWebsite(),
                org.getPlanTier(),
                org.getMaxMembers(),
                org.getCreatedAt(),
                org.getUpdatedAt());
    }
}
