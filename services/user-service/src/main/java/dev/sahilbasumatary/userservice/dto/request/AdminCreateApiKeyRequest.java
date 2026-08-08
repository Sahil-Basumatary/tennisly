package dev.sahilbasumatary.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminCreateApiKeyRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 100) String name,
        List<@NotBlank @Size(max = 64) String> scopes,
        Instant expiresAt) {}
