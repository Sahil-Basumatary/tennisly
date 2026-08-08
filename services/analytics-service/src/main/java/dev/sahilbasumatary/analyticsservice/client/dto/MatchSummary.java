package dev.sahilbasumatary.analyticsservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchSummary(
        UUID id,
        String externalId,
        UUID tournamentId,
        String surface,
        String status,
        int bestOfSets,
        Instant scheduledAt,
        Instant startedAt,
        Instant endedAt,
        Map<String, Object> metadata,
        Map<String, Object> currentScore,
        List<MatchPlayerSummary> players,
        int pointsPlayed) {}
