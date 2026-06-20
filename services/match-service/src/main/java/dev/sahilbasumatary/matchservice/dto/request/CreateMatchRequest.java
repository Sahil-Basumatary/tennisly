package dev.sahilbasumatary.matchservice.dto.request;

import dev.sahilbasumatary.matchservice.entity.Surface;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateMatchRequest(
        @Size(max = 255) String externalId,
        UUID tournamentId,
        @NotNull Surface surface,
        @Min(3) @Max(5) Integer bestOfSets,
        Instant scheduledAt,
        Map<String, Object> metadata,
        @Valid @NotNull @Size(min = 2, max = 2) List<CreateMatchPlayerRequest> players) {}
