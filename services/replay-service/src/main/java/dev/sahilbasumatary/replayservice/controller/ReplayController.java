package dev.sahilbasumatary.replayservice.controller;

import dev.sahilbasumatary.replayservice.dto.response.MatchReplayResponse;
import dev.sahilbasumatary.replayservice.dto.response.PointReplayResponse;
import dev.sahilbasumatary.replayservice.service.ReplayGenerationService;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replays")
@Validated
public class ReplayController {

    private static final Logger log = LoggerFactory.getLogger(ReplayController.class);

    private final ReplayGenerationService replayGenerationService;

    public ReplayController(ReplayGenerationService replayGenerationService) {
        this.replayGenerationService = replayGenerationService;
    }

    @GetMapping("/matches/{matchId}")
    public ResponseEntity<MatchReplayResponse> generateMatchReplay(@PathVariable UUID matchId) {
        log.debug("GET /api/replays/matches/{}", matchId);
        return ResponseEntity.ok(replayGenerationService.generateMatchReplay(matchId));
    }

    @GetMapping("/matches/{matchId}/points/{sequenceNumber}")
    public ResponseEntity<PointReplayResponse> generatePointReplay(
            @PathVariable UUID matchId, @PathVariable @Min(1) int sequenceNumber) {
        log.debug("GET /api/replays/matches/{}/points/{}", matchId, sequenceNumber);
        return ResponseEntity.ok(
                replayGenerationService.generatePointReplay(matchId, sequenceNumber));
    }
}
