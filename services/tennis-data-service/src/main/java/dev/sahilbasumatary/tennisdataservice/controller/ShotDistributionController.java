package dev.sahilbasumatary.tennisdataservice.controller;

import dev.sahilbasumatary.tennisdataservice.dto.response.ShotDistributionResponse;
import dev.sahilbasumatary.tennisdataservice.entity.PlayerTier;
import dev.sahilbasumatary.tennisdataservice.entity.ShotType;
import dev.sahilbasumatary.tennisdataservice.entity.Surface;
import dev.sahilbasumatary.tennisdataservice.service.ShotDistributionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tennis/shot-distributions")
public class ShotDistributionController {

    private final ShotDistributionService shotDistributionService;

    public ShotDistributionController(ShotDistributionService shotDistributionService) {
        this.shotDistributionService = shotDistributionService;
    }

    @GetMapping
    public ResponseEntity<List<ShotDistributionResponse>> listDistributions(
            @RequestParam(required = false) ShotType shotType,
            @RequestParam(required = false) Surface surface,
            @RequestParam(required = false) PlayerTier playerTier) {
        return ResponseEntity.ok(
                shotDistributionService.listDistributions(shotType, surface, playerTier));
    }

    @GetMapping("/{shotType}")
    public ResponseEntity<List<ShotDistributionResponse>> getByShotType(
            @PathVariable ShotType shotType) {
        return ResponseEntity.ok(shotDistributionService.getByShotType(shotType));
    }
}
