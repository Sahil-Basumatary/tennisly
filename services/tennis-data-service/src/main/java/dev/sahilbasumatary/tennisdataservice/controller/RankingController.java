package dev.sahilbasumatary.tennisdataservice.controller;

import dev.sahilbasumatary.tennisdataservice.dto.response.RankingResponse;
import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.RankingType;
import dev.sahilbasumatary.tennisdataservice.service.RankingService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tennis/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public ResponseEntity<List<RankingResponse>> getCurrentRankings(
            @RequestParam Gender gender,
            @RequestParam(defaultValue = "SINGLES") RankingType type) {
        return ResponseEntity.ok(rankingService.getCurrentRankings(gender, type));
    }
}
