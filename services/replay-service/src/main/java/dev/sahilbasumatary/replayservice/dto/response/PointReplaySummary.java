package dev.sahilbasumatary.replayservice.dto.response;

import dev.sahilbasumatary.replayservice.domain.PointOutcome;
import java.util.UUID;

public record PointReplaySummary(
        int sequence,
        UUID serverId,
        UUID winnerId,
        PointOutcome outcome,
        int rallyLength,
        int shotCount,
        double durationSeconds) {}
