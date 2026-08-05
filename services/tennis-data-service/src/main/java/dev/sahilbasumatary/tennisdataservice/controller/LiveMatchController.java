package dev.sahilbasumatary.tennisdataservice.controller;

import dev.sahilbasumatary.tennisdataservice.dto.response.UpstreamMatchResponse;
import dev.sahilbasumatary.tennisdataservice.dto.response.UpstreamPointResponse;
import dev.sahilbasumatary.tennisdataservice.service.LiveMatchQueryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tennis/matches")
public class LiveMatchController {

    private final LiveMatchQueryService liveMatchQueryService;

    public LiveMatchController(LiveMatchQueryService liveMatchQueryService) {
        this.liveMatchQueryService = liveMatchQueryService;
    }

    @GetMapping
    public ResponseEntity<List<UpstreamMatchResponse>> list(
            @RequestParam(defaultValue = "live") String status,
            @RequestParam(required = false) String tour,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safeOffset = Math.max(offset, 0);
        if ("completed".equalsIgnoreCase(status) || "history".equalsIgnoreCase(status)) {
            return ResponseEntity.ok(
                    liveMatchQueryService.listHistoryMatches(safeLimit, safeOffset));
        }
        return ResponseEntity.ok(
                liveMatchQueryService.listMatches(status.toLowerCase(), tour, safeLimit, safeOffset));
    }

    @GetMapping("/{ltaId}")
    public ResponseEntity<UpstreamMatchResponse> get(@PathVariable long ltaId) {
        return ResponseEntity.ok(liveMatchQueryService.getMatch(ltaId));
    }

    @GetMapping("/{ltaId}/points")
    public ResponseEntity<List<UpstreamPointResponse>> points(@PathVariable long ltaId) {
        return ResponseEntity.ok(liveMatchQueryService.getPoints(ltaId));
    }
}
