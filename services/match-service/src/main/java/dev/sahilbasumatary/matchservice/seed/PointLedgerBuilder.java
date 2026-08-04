package dev.sahilbasumatary.matchservice.seed;

import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchPoint;
import dev.sahilbasumatary.matchservice.entity.PointOutcome;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a compact but varied point ledger (aces, winners, errors, double faults) with score
 * snapshots a broadcast centre can render — enough for multi-point replay scrubbing without a full
 * 3-set PBP dump.
 */
final class PointLedgerBuilder {

    private PointLedgerBuilder() {}

    record Stroke(UUID serverId, UUID winnerId, PointOutcome outcome, int rallyLength) {}

    static List<MatchPoint> build(Match match, UUID homeId, UUID awayId, List<Stroke> strokes) {
        List<MatchPoint> points = new ArrayList<>(strokes.size());
        int homePoints = 0;
        int awayPoints = 0;
        int homeGames = 0;
        int awayGames = 0;
        int homeSets = 0;
        int awaySets = 0;
        List<Integer> homeSetScores = new ArrayList<>();
        List<Integer> awaySetScores = new ArrayList<>();
        int sequence = 1;
        for (Stroke stroke : strokes) {
            if (stroke.winnerId().equals(homeId)) {
                homePoints++;
            } else {
                awayPoints++;
            }
            boolean gameOver = false;
            if (homePoints >= 4 && homePoints - awayPoints >= 2) {
                homeGames++;
                homePoints = 0;
                awayPoints = 0;
                gameOver = true;
            } else if (awayPoints >= 4 && awayPoints - homePoints >= 2) {
                awayGames++;
                homePoints = 0;
                awayPoints = 0;
                gameOver = true;
            }
            if (gameOver && homeGames >= 6 && homeGames - awayGames >= 2) {
                homeSets++;
                homeSetScores.add(homeGames);
                awaySetScores.add(awayGames);
                homeGames = 0;
                awayGames = 0;
            } else if (gameOver && awayGames >= 6 && awayGames - homeGames >= 2) {
                awaySets++;
                homeSetScores.add(homeGames);
                awaySetScores.add(awayGames);
                homeGames = 0;
                awayGames = 0;
            }
            MatchPoint point = new MatchPoint();
            point.setSequenceNumber(sequence++);
            point.setServerId(stroke.serverId());
            point.setWinnerId(stroke.winnerId());
            point.setOutcome(stroke.outcome());
            point.setRallyLength(stroke.rallyLength());
            point.setScoreSnapshot(
                    snapshot(
                            homeId,
                            awayId,
                            homePoints,
                            awayPoints,
                            homeGames,
                            awayGames,
                            homeSetScores,
                            awaySetScores,
                            stroke.serverId()));
            point.setShotSummary(Map.of("rallyLength", stroke.rallyLength(), "outcome", stroke.outcome().name()));
            match.addPoint(point);
            points.add(point);
        }
        match.setCurrentScore(
                snapshot(
                        homeId,
                        awayId,
                        homePoints,
                        awayPoints,
                        homeGames,
                        awayGames,
                        homeSetScores,
                        awaySetScores,
                        strokes.isEmpty() ? homeId : strokes.get(strokes.size() - 1).serverId()));
        return points;
    }

    static List<Stroke> competitiveRally(UUID homeId, UUID awayId, int targetPoints) {
        List<Stroke> strokes = new ArrayList<>(targetPoints);
        UUID server = homeId;
        PointOutcome[] mix = {
            PointOutcome.WINNER,
            PointOutcome.FORCED_ERROR,
            PointOutcome.UNFORCED_ERROR,
            PointOutcome.ACE,
            PointOutcome.WINNER,
            PointOutcome.UNFORCED_ERROR,
            PointOutcome.FORCED_ERROR,
            PointOutcome.DOUBLE_FAULT,
            PointOutcome.ACE,
            PointOutcome.WINNER
        };
        int[] rallies = {1, 5, 8, 1, 12, 4, 7, 0, 1, 9};
        for (int i = 0; i < targetPoints; i++) {
            PointOutcome outcome = mix[i % mix.length];
            int rally =
                    outcome == PointOutcome.ACE || outcome == PointOutcome.DOUBLE_FAULT
                            ? (outcome == PointOutcome.ACE ? 1 : 0)
                            : rallies[i % rallies.length];
            UUID winner;
            if (outcome == PointOutcome.ACE) {
                winner = server;
            } else if (outcome == PointOutcome.DOUBLE_FAULT) {
                winner = server.equals(homeId) ? awayId : homeId;
            } else if (i % 3 == 0) {
                winner = server;
            } else {
                winner = i % 2 == 0 ? homeId : awayId;
            }
            strokes.add(new Stroke(server, winner, outcome, rally));
            // Change ends of serve every game-ish block of 4 points for variety.
            if ((i + 1) % 4 == 0) {
                server = server.equals(homeId) ? awayId : homeId;
            }
        }
        return strokes;
    }

    private static Map<String, Object> snapshot(
            UUID homeId,
            UUID awayId,
            int homePoints,
            int awayPoints,
            int homeGames,
            int awayGames,
            List<Integer> homeSetScores,
            List<Integer> awaySetScores,
            UUID serverId) {
        Map<String, Object> score = new LinkedHashMap<>();
        score.put("sets", List.of(Map.of("HOME", homeSetScores, "AWAY", awaySetScores)));
        score.put(
                "game",
                Map.of(
                        "HOME", pointLabel(homePoints),
                        "AWAY", pointLabel(awayPoints),
                        "homeGames", homeGames,
                        "awayGames", awayGames));
        score.put("serverId", serverId.toString());
        score.put(
                "players",
                List.of(
                        Map.of("playerId", homeId.toString(), "side", "HOME"),
                        Map.of("playerId", awayId.toString(), "side", "AWAY")));
        return score;
    }

    private static String pointLabel(int points) {
        return switch (points) {
            case 0 -> "0";
            case 1 -> "15";
            case 2 -> "30";
            case 3 -> "40";
            default -> "AD";
        };
    }
}
