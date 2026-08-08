package dev.sahilbasumatary.analyticsservice.query;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import dev.sahilbasumatary.analyticsservice.dto.response.CompareResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.HeadToHeadMeetingResponse;
import dev.sahilbasumatary.analyticsservice.index.MatchAnalyticsDocument;
import dev.sahilbasumatary.analyticsservice.index.PlayerMatchDocument;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HeadToHeadAggregator {

    private HeadToHeadAggregator() {}

    public static CompareResponse aggregate(
            UUID playerA,
            UUID playerB,
            List<PlayerMatchDocument> playerAMeetings,
            Map<UUID, MatchAnalyticsDocument> matchById) {
        int aWins = 0;
        int bWins = 0;
        int unknownResults = 0;
        int pointsWonA = 0;
        int servicePointsWonA = 0;
        int breakPointsWonA = 0;
        int pointsWonB = 0;
        int servicePointsWonB = 0;
        int breakPointsWonB = 0;
        List<HeadToHeadMeetingResponse> meetings = new ArrayList<>();
        List<PlayerMatchDocument> sorted = playerAMeetings.stream()
                .sorted(Comparator.comparing(
                                HeadToHeadAggregator::meetingInstant,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PlayerMatchDocument::getMatchId))
                .limit(AnalyticsQueryBounds.MAX_COMPARE_MEETINGS)
                .toList();
        for (PlayerMatchDocument meeting : sorted) {
            Boolean playerAWon = meeting.getWon();
            if (playerAWon == null) {
                unknownResults++;
            } else if (playerAWon) {
                aWins++;
            } else {
                bWins++;
            }
            TapeSideMetrics aMetrics = safeMetrics(meeting.getMetrics());
            pointsWonA += aMetrics.pointsWon();
            servicePointsWonA += aMetrics.servicePointsWon();
            breakPointsWonA += aMetrics.breakPointsWon();
            TapeSideMetrics bMetrics = resolveOpponentMetrics(meeting, playerB, matchById);
            pointsWonB += bMetrics.pointsWon();
            servicePointsWonB += bMetrics.servicePointsWon();
            breakPointsWonB += bMetrics.breakPointsWon();
            meetings.add(new HeadToHeadMeetingResponse(
                    meeting.getMatchId(),
                    meeting.getEndedAt(),
                    meeting.getScheduledAt(),
                    playerAWon,
                    meeting.getSurface(),
                    meeting.getTournamentName(),
                    aMetrics,
                    bMetrics));
        }
        return new CompareResponse(
                sorted.size(),
                aWins,
                bWins,
                unknownResults,
                meetings,
                new TapeSideMetrics(pointsWonA, servicePointsWonA, breakPointsWonA),
                new TapeSideMetrics(pointsWonB, servicePointsWonB, breakPointsWonB));
    }

    private static TapeSideMetrics resolveOpponentMetrics(
            PlayerMatchDocument meeting,
            UUID playerB,
            Map<UUID, MatchAnalyticsDocument> matchById) {
        MatchAnalyticsDocument match = matchById.get(meeting.getMatchId());
        if (match == null) {
            return new TapeSideMetrics(0, 0, 0);
        }
        if (playerB.equals(match.getHomePlayerId())) {
            return safeMetrics(match.getHomeMetrics());
        }
        if (playerB.equals(match.getAwayPlayerId())) {
            return safeMetrics(match.getAwayMetrics());
        }
        return new TapeSideMetrics(0, 0, 0);
    }

    private static TapeSideMetrics safeMetrics(TapeSideMetrics metrics) {
        return metrics == null ? new TapeSideMetrics(0, 0, 0) : metrics;
    }

    private static java.time.Instant meetingInstant(PlayerMatchDocument meeting) {
        return meeting.getEndedAt() != null ? meeting.getEndedAt() : meeting.getScheduledAt();
    }
}
