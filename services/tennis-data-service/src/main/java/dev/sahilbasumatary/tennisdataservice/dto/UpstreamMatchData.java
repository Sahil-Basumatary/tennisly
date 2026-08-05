package dev.sahilbasumatary.tennisdataservice.dto;

import java.time.Instant;
import java.util.List;

public record UpstreamMatchData(
        long providerMatchId,
        String externalId,
        String tournamentName,
        String surface,
        String format,
        String round,
        String status,
        boolean doubles,
        boolean indoor,
        Instant scheduledAt,
        UpstreamPlayerRef home,
        UpstreamPlayerRef away,
        Integer winnerSide,
        UpstreamScoreSnapshot score) {

    public record UpstreamPlayerRef(
            Long providerPlayerId, String firstName, String lastName, String displayName) {}

    public record UpstreamScoreSnapshot(
            List<Integer> setsP1,
            List<Integer> setsP2,
            List<List<Integer>> games,
            List<String> points,
            Integer serverSide,
            boolean tiebreak) {}
}
