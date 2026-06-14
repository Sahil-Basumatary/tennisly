package dev.sahilbasumatary.tennisdataservice.controller;

import dev.sahilbasumatary.tennisdataservice.dto.response.SyncResultResponse;
import dev.sahilbasumatary.tennisdataservice.service.DataSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tennis/sync")
public class DataSyncController {

    private final DataSyncService dataSyncService;

    public DataSyncController(DataSyncService dataSyncService) {
        this.dataSyncService = dataSyncService;
    }

    @PostMapping("/players")
    public ResponseEntity<SyncResultResponse> syncPlayers() {
        return ResponseEntity.ok(
                SyncResultResponse.of("players", dataSyncService.syncPlayers()));
    }

    @PostMapping("/tournaments")
    public ResponseEntity<SyncResultResponse> syncTournaments() {
        return ResponseEntity.ok(
                SyncResultResponse.of("tournaments", dataSyncService.syncTournaments()));
    }

    @PostMapping("/rankings")
    public ResponseEntity<SyncResultResponse> syncRankings() {
        return ResponseEntity.ok(
                SyncResultResponse.of("rankings", dataSyncService.syncRankings()));
    }
}
