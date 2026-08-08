package dev.sahilbasumatary.analyticsservice.controller;

import dev.sahilbasumatary.analyticsservice.dto.response.CompareResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.MatchAnalyticsResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.MatchReportResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.PlayerAnalyticsResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.PlayerTrendsResponse;
import dev.sahilbasumatary.analyticsservice.dto.response.TournamentAnalyticsResponse;
import dev.sahilbasumatary.analyticsservice.query.AnalyticsQueryBounds;
import dev.sahilbasumatary.analyticsservice.service.AnalyticsQueryService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsQueryController {

    private final AnalyticsQueryService queryService;

    public AnalyticsQueryController(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "analytics-service");
    }

    @GetMapping("/matches/{matchId}")
    public MatchAnalyticsResponse getMatchAnalytics(@PathVariable UUID matchId) {
        return queryService.getMatchAnalytics(matchId);
    }

    @GetMapping("/players/{playerId}")
    public PlayerAnalyticsResponse getPlayerAnalytics(
            @PathVariable UUID playerId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String surface,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int resolvedPage = AnalyticsQueryBounds.clampPage(page);
        int resolvedSize = AnalyticsQueryBounds.clampPageSize(size);
        return queryService.getPlayerAnalytics(
                playerId, from, to, surface, resolvedPage, resolvedSize);
    }

    @GetMapping("/players/{playerId}/trends")
    public PlayerTrendsResponse getPlayerTrends(
            @PathVariable UUID playerId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String surface,
            @RequestParam(required = false) Integer size) {
        return queryService.getPlayerTrends(
                playerId, from, to, surface, AnalyticsQueryBounds.clampTrendSize(size));
    }

    @GetMapping("/compare")
    public CompareResponse comparePlayers(
            @RequestParam String playerA,
            @RequestParam String playerB,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        UUID resolvedA = AnalyticsQueryBounds.requireUuid(playerA, "playerA");
        UUID resolvedB = AnalyticsQueryBounds.requireUuid(playerB, "playerB");
        return queryService.comparePlayers(resolvedA, resolvedB, from, to);
    }

    @GetMapping("/tournaments/{tournamentKey}")
    public TournamentAnalyticsResponse getTournamentAnalytics(
            @PathVariable String tournamentKey) {
        return queryService.getTournamentAnalytics(tournamentKey);
    }

    @GetMapping(value = "/matches/{matchId}/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportMatchCsv(@PathVariable UUID matchId) {
        String csv = queryService.exportMatchCsv(matchId);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"match-" + matchId + "-analytics.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping(value = "/players/{playerId}/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportPlayerCsv(
            @PathVariable UUID playerId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String surface) {
        String csv = queryService.exportPlayerCsv(playerId, from, to, surface);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"player-" + playerId + "-analytics.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/matches/{matchId}/report")
    public MatchReportResponse getMatchReport(@PathVariable UUID matchId) {
        return queryService.getMatchReport(matchId);
    }
}
