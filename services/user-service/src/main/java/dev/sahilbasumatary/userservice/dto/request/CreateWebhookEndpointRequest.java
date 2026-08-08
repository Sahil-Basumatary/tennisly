package dev.sahilbasumatary.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateWebhookEndpointRequest(
        UUID organizationId,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 2048) String targetUrl,
        @NotEmpty List<@NotBlank String> eventTypes,
        @Size(max = 500) String description) {}
