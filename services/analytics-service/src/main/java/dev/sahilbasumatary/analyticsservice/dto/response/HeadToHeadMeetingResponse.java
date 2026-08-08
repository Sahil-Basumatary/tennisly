package dev.sahilbasumatary.analyticsservice.dto.response;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HeadToHeadMeetingResponse(
        UUID matchId,
        Instant endedAt,
        Instant scheduledAt,
        Boolean playerAWon,
        String surface,
        String tournamentName,
        TapeSideMetrics playerAMetrics,
        TapeSideMetrics playerBMetrics) {}
