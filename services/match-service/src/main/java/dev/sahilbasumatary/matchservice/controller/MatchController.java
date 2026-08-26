package dev.sahilbasumatary.matchservice.controller;

import dev.sahilbasumatary.matchservice.dto.request.CreateMatchRequest;
import dev.sahilbasumatary.matchservice.dto.request.RecordPointRequest;
import dev.sahilbasumatary.matchservice.dto.request.UpdateMatchRequest;
import dev.sahilbasumatary.matchservice.dto.request.UpdateMatchStatusRequest;
import dev.sahilbasumatary.matchservice.dto.response.MatchCursorResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchEventLogResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchLiveScoreResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchPointResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.service.MatchService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private static final Logger log = LoggerFactory.getLogger(MatchController.class);
    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<MatchResponse> createMatch(
            @Valid @RequestBody CreateMatchRequest request) {
        log.debug("POST /api/matches");
        return ResponseEntity.status(HttpStatus.CREATED).body(matchService.createMatch(request));
    }

    @GetMapping
    public ResponseEntity<List<MatchResponse>> listMatches(
            @RequestParam(required = false) MatchStatus status,
            @RequestParam(required = false) UUID tournamentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        log.debug("GET /api/matches status={} tournamentId={}", status, tournamentId);
        return ResponseEntity.ok(matchService.listMatches(status, tournamentId, page, size));
    }

    @GetMapping("/ticker")
    public ResponseEntity<List<MatchResponse>> ticker() {
        log.debug("GET /api/matches/ticker");
        return MatchPublicCache.ticker(matchService.listTicker());
    }

    @GetMapping("/external/{externalId}")
    public ResponseEntity<MatchResponse> getMatchByExternalId(@PathVariable String externalId) {
        log.debug("GET /api/matches/external/{}", externalId);
        return ResponseEntity.ok(matchService.getMatchByExternalId(externalId));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable UUID matchId) {
        log.debug("GET /api/matches/{}", matchId);
        return ResponseEntity.ok(matchService.getMatch(matchId));
    }

    @GetMapping("/{matchId}/live")
    public ResponseEntity<MatchLiveScoreResponse> getLiveScore(@PathVariable UUID matchId) {
        log.debug("GET /api/matches/{}/live", matchId);
        MatchLiveScoreResponse live = matchService.getLiveScore(matchId);
        return MatchPublicCache.withEtag(
                live,
                "live-" + live.id() + "-" + live.liveSequence(),
                MatchPublicCache.liveControl(live.status()));
    }

    @GetMapping("/{matchId}/cursor")
    public ResponseEntity<MatchCursorResponse> getLiveCursor(@PathVariable UUID matchId) {
        log.debug("GET /api/matches/{}/cursor", matchId);
        MatchCursorResponse cursor = matchService.getLiveCursor(matchId);
        return MatchPublicCache.withEtag(
                cursor,
                "cursor-" + cursor.id() + "-" + cursor.liveSequence(),
                MatchPublicCache.liveControl(cursor.status()));
    }

    @PutMapping("/{matchId}")
    public ResponseEntity<MatchResponse> updateMatch(
            @PathVariable UUID matchId, @Valid @RequestBody UpdateMatchRequest request) {
        log.debug("PUT /api/matches/{}", matchId);
        return ResponseEntity.ok(matchService.updateMatch(matchId, request));
    }

    @PatchMapping("/{matchId}/status")
    public ResponseEntity<MatchResponse> updateStatus(
            @PathVariable UUID matchId, @Valid @RequestBody UpdateMatchStatusRequest request) {
        log.debug("PATCH /api/matches/{}/status", matchId);
        return ResponseEntity.ok(matchService.updateStatus(matchId, request));
    }

    @PostMapping("/{matchId}/points")
    public ResponseEntity<MatchPointResponse> recordPoint(
            @PathVariable UUID matchId, @Valid @RequestBody RecordPointRequest request) {
        log.debug("POST /api/matches/{}/points", matchId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchService.recordPoint(matchId, request));
    }

    @GetMapping("/{matchId}/points")
    public ResponseEntity<List<MatchPointResponse>> listPoints(@PathVariable UUID matchId) {
        log.debug("GET /api/matches/{}/points", matchId);
        return ResponseEntity.ok(matchService.listPoints(matchId));
    }

    @GetMapping("/{matchId}/events")
    public ResponseEntity<List<MatchEventLogResponse>> listEvents(
            @PathVariable UUID matchId,
            @RequestParam(defaultValue = "0") long afterSequence,
            @RequestParam(defaultValue = "100") int limit) {
        log.debug(
                "GET /api/matches/{}/events afterSequence={} limit={}",
                matchId,
                afterSequence,
                limit);
        return ResponseEntity.ok()
                .header("Cache-Control", MatchPublicCache.PRIVATE_NO_STORE)
                .body(matchService.listEvents(matchId, afterSequence, limit));
    }
}
