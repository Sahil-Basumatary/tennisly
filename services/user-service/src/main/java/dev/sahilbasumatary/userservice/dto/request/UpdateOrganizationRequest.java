package dev.sahilbasumatary.userservice.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @Size(max = 512) String logoUrl,
        @Size(max = 512) String website) {}
