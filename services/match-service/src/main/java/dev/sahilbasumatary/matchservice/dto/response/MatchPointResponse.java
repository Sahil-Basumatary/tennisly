package dev.sahilbasumatary.matchservice.dto.response;

import dev.sahilbasumatary.matchservice.entity.MatchPoint;
import dev.sahilbasumatary.matchservice.entity.PointOutcome;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MatchPointResponse(
        UUID id,
        int sequenceNumber,
        UUID serverId,
        UUID winnerId,
        PointOutcome outcome,
        int rallyLength,
        Map<String, Object> scoreSnapshot,
        Map<String, Object> shotSummary,
        Instant recordedAt) {

    public static MatchPointResponse from(MatchPoint point) {
        return new MatchPointResponse(
                point.getId(),
                point.getSequenceNumber(),
                point.getServerId(),
                point.getWinnerId(),
                point.getOutcome(),
                point.getRallyLength(),
                point.getScoreSnapshot(),
                point.getShotSummary(),
                point.getRecordedAt());
    }
}
