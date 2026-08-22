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
        UUID homeId = null;
        UUID awayId = null;
        for (MatchPlayerSummary player : match.players()) {
            if ("HOME".equals(player.side())) {
                homeId = player.playerId();
            } else if ("AWAY".equals(player.side())) {
                awayId = player.playerId();
            }
        }
        if (homeId == null || awayId == null) {
            throw new IllegalArgumentException("Match players incomplete for stats");
        }
        int homePointsWon = 0;
        int homeServicePointsWon = 0;
        int homeBreakPointsWon = 0;
        int awayPointsWon = 0;
        int awayServicePointsWon = 0;
        int awayBreakPointsWon = 0;
        for (MatchPointSummary point : points) {
            boolean awayWon = point.winnerId().equals(awayId);
            if (awayWon) {
                awayPointsWon += 1;
            } else {
                homePointsWon += 1;
            }
            if (point.winnerId().equals(point.serverId())) {
                if (awayWon) {
                    awayServicePointsWon += 1;
                } else {
                    homeServicePointsWon += 1;
                }
            } else if (isServiceBreak(point)) {
                if (awayWon) {
                    awayBreakPointsWon += 1;
                } else {
                    homeBreakPointsWon += 1;
                }
            }
        }
        TapeSideMetrics home =
                new TapeSideMetrics(homePointsWon, homeServicePointsWon, homeBreakPointsWon);
        TapeSideMetrics away =
                new TapeSideMetrics(awayPointsWon, awayServicePointsWon, awayBreakPointsWon);
        return new TapeMatchMetrics(home, away, points.size());
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

}
