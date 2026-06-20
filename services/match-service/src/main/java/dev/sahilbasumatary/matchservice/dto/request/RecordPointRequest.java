package dev.sahilbasumatary.matchservice.dto.request;

import dev.sahilbasumatary.matchservice.entity.PointOutcome;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record RecordPointRequest(
        @NotNull UUID serverId,
        @NotNull UUID winnerId,
        @NotNull PointOutcome outcome,
        @Min(0) int rallyLength,
        @NotNull Map<String, Object> scoreSnapshot,
        Map<String, Object> shotSummary) {}
