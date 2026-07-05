package dev.sahilbasumatary.replayservice.controller;

import dev.sahilbasumatary.replayservice.dto.response.MatchReplayResponse;
import dev.sahilbasumatary.replayservice.dto.response.PointReplayResponse;
import dev.sahilbasumatary.replayservice.dto.response.ReplayArtifactResponse;
import dev.sahilbasumatary.replayservice.service.ReplayArtifactService;
import dev.sahilbasumatary.replayservice.service.ReplayGenerationService;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replays")
@Validated
public class ReplayController {

    private static final Logger log = LoggerFactory.getLogger(ReplayController.class);

    private final ReplayGenerationService replayGenerationService;
    private final ReplayArtifactService replayArtifactService;

    public ReplayController(
            ReplayGenerationService replayGenerationService,
            ReplayArtifactService replayArtifactService) {
        this.replayGenerationService = replayGenerationService;
        this.replayArtifactService = replayArtifactService;
    }

    @GetMapping("/matches/{matchId}")
    public ResponseEntity<MatchReplayResponse> getMatchReplay(@PathVariable UUID matchId) {
        log.debug("GET /api/replays/matches/{}", matchId);
        return ResponseEntity.ok(replayArtifactService.getReplay(matchId));
    }

    @PostMapping("/matches/{matchId}")
    public ResponseEntity<ReplayArtifactResponse> materializeMatchReplay(
            @PathVariable UUID matchId) {
        log.debug("POST /api/replays/matches/{}", matchId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(replayArtifactService.materialize(matchId));
    }

    @GetMapping("/matches/{matchId}/artifact")
    public ResponseEntity<ReplayArtifactResponse> getArtifact(@PathVariable UUID matchId) {
        log.debug("GET /api/replays/matches/{}/artifact", matchId);
        return ResponseEntity.ok(replayArtifactService.getArtifact(matchId));
    }

    @DeleteMapping("/matches/{matchId}/artifact")
    public ResponseEntity<Void> deleteArtifact(@PathVariable UUID matchId) {
        log.debug("DELETE /api/replays/matches/{}/artifact", matchId);
        replayArtifactService.delete(matchId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/matches/{matchId}/points/{sequenceNumber}")
    public ResponseEntity<PointReplayResponse> generatePointReplay(
            @PathVariable UUID matchId, @PathVariable @Min(1) int sequenceNumber) {
        log.debug("GET /api/replays/matches/{}/points/{}", matchId, sequenceNumber);
        return ResponseEntity.ok(
                replayGenerationService.generatePointReplay(matchId, sequenceNumber));
    }
}
