package dev.sahilbasumatary.tennisdataservice.dto.response;

import dev.sahilbasumatary.tennisdataservice.dto.UpstreamMatchData;
import java.time.Instant;
import java.util.List;

public record UpstreamMatchResponse(
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
        PlayerSide home,
        PlayerSide away,
        Integer winnerSide,
        ScoreSnapshot score) {

    public record PlayerSide(
            Long providerPlayerId, String firstName, String lastName, String displayName) {}

    public record ScoreSnapshot(
            List<Integer> sets,
            List<List<Integer>> games,
            List<String> points,
            Integer serverSide,
            boolean tiebreak) {}

    public static UpstreamMatchResponse from(UpstreamMatchData match) {
        return new UpstreamMatchResponse(
                match.providerMatchId(),
                match.externalId(),
                match.tournamentName(),
                match.surface(),
                match.format(),
                match.round(),
                match.status(),
                match.doubles(),
                match.indoor(),
                match.scheduledAt(),
                toSide(match.home()),
                toSide(match.away()),
                match.winnerSide(),
                toScore(match.score()));
    }

    private static ScoreSnapshot toScore(UpstreamMatchData.UpstreamScoreSnapshot score) {
        if (score == null) {
            return null;
        }
        return new ScoreSnapshot(
                score.sets(), score.games(), score.points(), score.serverSide(), score.tiebreak());
    }

    private static PlayerSide toSide(UpstreamMatchData.UpstreamPlayerRef ref) {
        if (ref == null) {
            return new PlayerSide(null, "", "", "Unknown");
        }
        return new PlayerSide(
                ref.providerPlayerId(), ref.firstName(), ref.lastName(), ref.displayName());
    }
}
