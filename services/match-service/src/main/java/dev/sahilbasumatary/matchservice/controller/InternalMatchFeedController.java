package dev.sahilbasumatary.matchservice.controller;

import dev.sahilbasumatary.matchservice.dto.response.CompletedMatchFeedResponse;
import dev.sahilbasumatary.matchservice.service.MatchService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/matches")
public class InternalMatchFeedController {

    private static final Logger log = LoggerFactory.getLogger(InternalMatchFeedController.class);
    private final MatchService matchService;

    public InternalMatchFeedController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/completed")
    public CompletedMatchFeedResponse listCompleted(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "50") int limit) {
        log.debug("GET /internal/matches/completed cursor={} limit={}", cursor, limit);
        return matchService.listCompletedMatchIds(cursor, limit);
    }
}
