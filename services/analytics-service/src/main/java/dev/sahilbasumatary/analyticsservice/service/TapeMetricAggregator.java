package dev.sahilbasumatary.analyticsservice.service;

import dev.sahilbasumatary.analyticsservice.client.dto.MatchPointSummary;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchPlayerSummary;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchSummary;
import dev.sahilbasumatary.analyticsservice.domain.TapeMatchMetrics;
import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TapeMetricAggregator {

    public TapeMatchMetrics aggregate(MatchSummary match, List<MatchPointSummary> points) {
        SideBag sides = sideIds(match.players());
        MutableBucket home = new MutableBucket();
        MutableBucket away = new MutableBucket();
        for (MatchPointSummary point : points) {
            bucketFor(sides, point.winnerId(), home, away).pointsWon += 1;
            if (point.winnerId().equals(point.serverId())) {
                bucketFor(sides, point.serverId(), home, away).servicePointsWon += 1;
            } else if (isServiceBreak(point)) {
                bucketFor(sides, point.winnerId(), home, away).breakPointsWon += 1;
            }
        }
        return new TapeMatchMetrics(home.toMetrics(), away.toMetrics(), points.size());
    }

    private static SideBag sideIds(List<MatchPlayerSummary> players) {
        MatchPlayerSummary home =
                players.stream().filter(player -> "HOME".equals(player.side())).findFirst().orElse(null);
        MatchPlayerSummary away =
                players.stream().filter(player -> "AWAY".equals(player.side())).findFirst().orElse(null);
        if (home == null || away == null) {
            throw new IllegalArgumentException("Match players incomplete for stats");
        }
        return new SideBag(home.playerId(), away.playerId());
    }

    private static MutableBucket bucketFor(
            SideBag sides, UUID playerId, MutableBucket home, MutableBucket away) {
        return playerId.equals(sides.awayId()) ? away : home;
    }

    private static boolean isServiceBreak(MatchPointSummary point) {
        if (point.winnerId().equals(point.serverId())) {
            return false;
        }
        Map<String, Object> snapshot = point.scoreSnapshot();
        if (snapshot == null) {
            return false;
        }
        Object pts = snapshot.get("points");
        if (!(pts instanceof List<?> list) || list.size() < 2) {
            return false;
        }
        return "0".equals(String.valueOf(list.get(0))) && "0".equals(String.valueOf(list.get(1)));
    }

    private record SideBag(UUID homeId, UUID awayId) {}

    private static final class MutableBucket {
        int pointsWon;
        int servicePointsWon;
        int breakPointsWon;

        TapeSideMetrics toMetrics() {
            return new TapeSideMetrics(pointsWon, servicePointsWon, breakPointsWon);
        }
    }
}
