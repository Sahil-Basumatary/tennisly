package dev.sahilbasumatary.matchservice.dto.request;

import dev.sahilbasumatary.matchservice.entity.Surface;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UpdateMatchRequest(
        @Size(max = 255) String externalId,
        UUID tournamentId,
        Surface surface,
        @Min(3) @Max(5) Integer bestOfSets,
        Instant scheduledAt,
        Map<String, Object> metadata) {}
