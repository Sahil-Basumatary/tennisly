package dev.sahilbasumatary.analyticsservice.service;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import dev.sahilbasumatary.analyticsservice.dto.response.CompareResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.MatchAnalyticsResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.MatchReportResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.MatchReportSectionResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.PlayerAnalyticsResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.PlayerMatchRowResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.PlayerSummaryResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.PlayerTrendPointResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.PlayerTrendsResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.TournamentAnalyticsResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.TournamentTopPlayerResponse;
import dev.sahilbasumatary.analyticsservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.analyticsservice.index.MatchAnalyticsDocument;
import dev.sahilbasumatary.analyticsservice.index.PlayerMatchDocument;
import dev.sahilbasumatary.analyticsservice.query.AnalyticsCsvFormatter;
import dev.sahilbasumatary.analyticsservice.query.AnalyticsQueryBounds;
import dev.sahilbasumatary.analyticsservice.query.HeadToHeadAggregator;
import dev.sahilbasumatary.analyticsservice.query.PlayerSummaryAggregator;
import dev.sahilbasumatary.analyticsservice.repository.AnalyticsReadRepository;
import dev.sahilbasumatary.analyticsservice.repository.AnalyticsReadRepository.PlayerMatchQuery;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsQueryService {

    private final AnalyticsReadRepository readRepository;

    public AnalyticsQueryService(AnalyticsReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    public MatchAnalyticsResponse getMatchAnalytics(UUID matchId) {
        MatchAnalyticsDocument document =
                readRepository
                        .findMatchById(matchId)
                        .orElseThrow(() -> new ResourceNotFoundException("Match analytics", matchId));
        return MatchAnalyticsResponse.from(document);
    }

    public PlayerAnalyticsResponse getPlayerAnalytics(
            UUID playerId, Instant from, Instant to, String surface, int page, int size) {
        AnalyticsQueryBounds.validateDateRange(from, to);
        PlayerMatchQuery query = new PlayerMatchQuery(playerId, null, from, to, surface);
        SearchHits<PlayerMatchDocument> pageHits =
                readRepository.searchPlayerMatches(
                        query, page, size, AnalyticsReadRepository.recentMatchSort());
        List<PlayerMatchDocument> summarySource =
                readRepository.scanPlayerMatches(
                        query,
                        PlayerSummaryAggregator.summaryScanCap(),
                        AnalyticsReadRepository.recentMatchSort());
        PlayerSummaryResponse summary = PlayerSummaryAggregator.aggregate(summarySource);
        List<PlayerMatchRowResponse> rows =
                pageHits.getSearchHits().stream()
                        .map(hit -> PlayerMatchRowResponse.from(hit.getContent()))
                        .toList();
        return new PlayerAnalyticsResponse(
                playerId, summary, rows, page, size, pageHits.getTotalHits());
    }

    public PlayerTrendsResponse getPlayerTrends(
            UUID playerId, Instant from, Instant to, String surface, int size) {
        AnalyticsQueryBounds.validateDateRange(from, to);
        int cappedSize = AnalyticsQueryBounds.clampTrendSize(size);
        PlayerMatchQuery query = new PlayerMatchQuery(playerId, null, from, to, surface);
        List<PlayerMatchDocument> trends =
                readRepository.scanPlayerMatches(
                        query, cappedSize, AnalyticsReadRepository.chronologicalSort());
        List<PlayerTrendPointResponse> points =
                trends.stream().map(PlayerTrendPointResponse::from).toList();
        return new PlayerTrendsResponse(playerId, points);
    }

    public CompareResponse comparePlayers(
            UUID playerA, UUID playerB, Instant from, Instant to) {
        AnalyticsQueryBounds.validateDateRange(from, to);
        PlayerMatchQuery query = new PlayerMatchQuery(playerA, playerB, from, to, null);
        List<PlayerMatchDocument> meetings =
                readRepository.scanPlayerMatches(
                        query,
                        AnalyticsQueryBounds.MAX_COMPARE_MEETINGS,
                        AnalyticsReadRepository.recentMatchSort());
        List<UUID> matchIds =
                meetings.stream().map(PlayerMatchDocument::getMatchId).distinct().toList();
        Map<UUID, MatchAnalyticsDocument> matchById = readRepository.mapMatchesById(matchIds);
        return HeadToHeadAggregator.aggregate(playerA, playerB, meetings, matchById);
    }

    public TournamentAnalyticsResponse getTournamentAnalytics(String tournamentKey) {
        long matchCount = readRepository.countMatchesByTournamentKey(tournamentKey);
        if (matchCount == 0) {
            return new TournamentAnalyticsResponse(
                    tournamentKey, null, null, 0, Map.of(), List.of());
        }
        List<MatchAnalyticsDocument> matches =
                readRepository.findMatchesByTournamentKey(
                        tournamentKey, PlayerSummaryAggregator.summaryScanCap());
        String tournamentName = matches.stream()
                .map(MatchAnalyticsDocument::getTournamentName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
        Integer season = matches.stream()
                .map(MatchAnalyticsDocument::getSeason)
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
        Map<String, Long> surfaceBreakdown = matches.stream()
                .collect(
                        Collectors.groupingBy(
                                match -> match.getSurface() == null ? "UNKNOWN" : match.getSurface(),
                                Collectors.counting()));
        Map<UUID, PlayerAccumulator> playerTotals = new HashMap<>();
        for (MatchAnalyticsDocument match : matches) {
            accumulatePlayer(playerTotals, match.getHomePlayerId(), match.getHomeDisplayName(), match.getHomeMetrics());
            accumulatePlayer(playerTotals, match.getAwayPlayerId(), match.getAwayDisplayName(), match.getAwayMetrics());
        }
        List<TournamentTopPlayerResponse> topPlayers =
                playerTotals.values().stream()
                        .sorted(Comparator.comparingInt(PlayerAccumulator::pointsWon).reversed())
                        .limit(AnalyticsQueryBounds.MAX_TOP_PLAYERS)
                        .map(
                                entry ->
                                        new TournamentTopPlayerResponse(
                                                entry.playerId(), entry.displayName(), entry.pointsWon()))
                        .toList();
        return new TournamentAnalyticsResponse(
                tournamentKey,
                tournamentName,
                season,
                matchCount,
                surfaceBreakdown,
                topPlayers);
    }

    public String exportMatchCsv(UUID matchId) {
        MatchAnalyticsDocument document =
                readRepository
                        .findMatchById(matchId)
                        .orElseThrow(() -> new ResourceNotFoundException("Match analytics", matchId));
        return AnalyticsCsvFormatter.formatMatchCsv(document);
    }

    public String exportPlayerCsv(
            UUID playerId, Instant from, Instant to, String surface) {
        AnalyticsQueryBounds.validateDateRange(from, to);
        PlayerMatchQuery query = new PlayerMatchQuery(playerId, null, from, to, surface);
        List<PlayerMatchDocument> rows =
                readRepository.exportPlayerMatches(query, AnalyticsQueryBounds.MAX_EXPORT_ROWS);
        return AnalyticsCsvFormatter.formatPlayerMatchesCsv(rows);
    }

    public MatchReportResponse getMatchReport(UUID matchId) {
        MatchAnalyticsResponse match = getMatchAnalytics(matchId);
        String title = buildReportTitle(match);
        List<MatchReportSectionResponse> sections =
                List.of(
                        new MatchReportSectionResponse(
                                "overview",
                                "Match Overview",
                                Map.of(
                                        "tournament",
                                        match.tournamentName(),
                                        "surface",
                                        match.surface(),
                                        "status",
                                        match.status(),
                                        "pointsPlayed",
                                        match.pointsPlayed())),
                        new MatchReportSectionResponse(
                                "players",
                                "Players",
                                Map.of(
                                        "home",
                                        Map.of(
                                                "id",
                                                match.homePlayerId(),
                                                "name",
                                                match.homeDisplayName(),
                                                "metrics",
                                                match.homeMetrics()),
                                        "away",
                                        Map.of(
                                                "id",
                                                match.awayPlayerId(),
                                                "name",
                                                match.awayDisplayName(),
                                                "metrics",
                                                match.awayMetrics()))),
                        new MatchReportSectionResponse(
                                "score",
                                "Score Snapshot",
                                match.scoreSnapshot() == null ? Map.of() : match.scoreSnapshot()));
        return new MatchReportResponse(Instant.now(), title, sections, match);
    }

    private static String buildReportTitle(MatchAnalyticsResponse match) {
        String home = match.homeDisplayName() == null ? "Home" : match.homeDisplayName();
        String away = match.awayDisplayName() == null ? "Away" : match.awayDisplayName();
        if (match.tournamentName() != null && !match.tournamentName().isBlank()) {
            return home + " vs " + away + " — " + match.tournamentName();
        }
        return home + " vs " + away;
    }

    private static void accumulatePlayer(
            Map<UUID, PlayerAccumulator> totals,
            UUID playerId,
            String displayName,
            TapeSideMetrics metrics) {
        if (playerId == null) {
            return;
        }
        TapeSideMetrics safe = metrics == null ? new TapeSideMetrics(0, 0, 0) : metrics;
        PlayerAccumulator accumulator =
                totals.computeIfAbsent(
                        playerId,
                        ignored -> new PlayerAccumulator(playerId, displayName == null ? "Unknown" : displayName));
        if (displayName != null && !displayName.isBlank()) {
            accumulator.displayName = displayName;
        }
        accumulator.pointsWon += safe.pointsWon();
    }

    private static final class PlayerAccumulator {
        private final UUID playerId;
        private String displayName;
        private int pointsWon;

        private PlayerAccumulator(UUID playerId, String displayName) {
            this.playerId = playerId;
            this.displayName = displayName;
        }

        private UUID playerId() {
            return playerId;
        }

        private String displayName() {
            return displayName;
        }

        private int pointsWon() {
            return pointsWon;
        }
    }
}
