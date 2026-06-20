package dev.sahilbasumatary.matchservice.controller;

import dev.sahilbasumatary.matchservice.dto.request.CreateMatchRequest;
import dev.sahilbasumatary.matchservice.dto.request.RecordPointRequest;
import dev.sahilbasumatary.matchservice.dto.request.UpdateMatchRequest;
import dev.sahilbasumatary.matchservice.dto.request.UpdateMatchStatusRequest;
import dev.sahilbasumatary.matchservice.dto.response.MatchEventLogResponse;
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
            @RequestParam(required = false) MatchStatus status) {
        log.debug("GET /api/matches status={}", status);
        return ResponseEntity.ok(matchService.listMatches(status));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable UUID matchId) {
        log.debug("GET /api/matches/{}", matchId);
        return ResponseEntity.ok(matchService.getMatch(matchId));
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
    public ResponseEntity<List<MatchEventLogResponse>> listEvents(@PathVariable UUID matchId) {
        log.debug("GET /api/matches/{}/events", matchId);
        return ResponseEntity.ok(matchService.listEvents(matchId));
    }
}
