package dev.sahilbasumatary.userservice.dto.request;

import dev.sahilbasumatary.userservice.entity.PlanTier;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminUpdateOrganizationRequest(
        @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @Size(max = 512) String logoUrl,
        @Size(max = 512) String website,
        PlanTier planTier,
        @Min(1) Integer maxMembers,
        Boolean active) {}
