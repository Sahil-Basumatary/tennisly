package dev.sahilbasumatary.tennisdataservice.controller;

import dev.sahilbasumatary.tennisdataservice.dto.response.PlayerResponse;
import dev.sahilbasumatary.tennisdataservice.dto.response.RankingResponse;
import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.service.PlayerService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tennis/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public ResponseEntity<List<PlayerResponse>> listPlayers(
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) String nationality) {
        return ResponseEntity.ok(playerService.listPlayers(gender, nationality));
    }

    @GetMapping("/external/{externalId}")
    public ResponseEntity<PlayerResponse> getPlayerByExternalId(@PathVariable String externalId) {
        return ResponseEntity.ok(playerService.getPlayerByExternalId(externalId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> getPlayer(@PathVariable UUID id) {
        return ResponseEntity.ok(playerService.getPlayer(id));
    }

    @GetMapping("/{id}/rankings")
    public ResponseEntity<List<RankingResponse>> getPlayerRankings(@PathVariable UUID id) {
        return ResponseEntity.ok(playerService.getPlayerRankingHistory(id));
    }
}
