package dev.sahilbasumatary.analyticsservice.controller;

import dev.sahilbasumatary.analyticsservice.service.MatchEventAnalyticsHandler;
import dev.sahilbasumatary.common.event.MatchEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/events")
public class InternalAnalyticsEventController {

    private final MatchEventAnalyticsHandler matchEventAnalyticsHandler;

    public InternalAnalyticsEventController(MatchEventAnalyticsHandler matchEventAnalyticsHandler) {
        this.matchEventAnalyticsHandler = matchEventAnalyticsHandler;
    }

    @PostMapping("/matches")
    public ResponseEntity<Void> ingestMatch(@RequestBody MatchEvent event) {
        matchEventAnalyticsHandler.handle(event);
        return ResponseEntity.noContent().build();
    }
}
