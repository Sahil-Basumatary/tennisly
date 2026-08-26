package dev.sahilbasumatary.replayservice.dto.response;

import dev.sahilbasumatary.replayservice.config.ReplayEngineVersions;
import dev.sahilbasumatary.replayservice.domain.Surface;
import java.util.List;
import java.util.UUID;

/** Full synthetic replay for a match: ordered point summaries, shot overlays and rendered frames. */
public record MatchReplayResponse(
        UUID matchId,
        Surface surface,
        int frameRate,
        int pointCount,
        int shotCount,
        int frameCount,
        double durationSeconds,
        List<PointReplaySummary> points,
        List<ShotSummaryResponse> shots,
        List<ReplayFrame> frames,
        String engineVersion) {

    public MatchReplayResponse {
        if (engineVersion == null || engineVersion.isBlank()) {
            engineVersion = ReplayEngineVersions.V1;
        }
    }
}
