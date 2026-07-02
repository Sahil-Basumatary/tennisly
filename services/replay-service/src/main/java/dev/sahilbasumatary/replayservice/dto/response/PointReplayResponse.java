package dev.sahilbasumatary.replayservice.dto.response;

import dev.sahilbasumatary.replayservice.domain.Surface;
import java.util.List;
import java.util.UUID;

/** Synthetic replay for a single point. */
public record PointReplayResponse(
        UUID matchId,
        Surface surface,
        int frameRate,
        PointReplaySummary point,
        List<ShotSummaryResponse> shots,
        List<ReplayFrame> frames) {}
