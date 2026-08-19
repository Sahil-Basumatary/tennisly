package dev.sahilbasumatary.replayservice.controller;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.replayservice.service.MatchCompletedReplayHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/events")
public class InternalReplayEventController {

    private final MatchCompletedReplayHandler matchCompletedReplayHandler;

    public InternalReplayEventController(MatchCompletedReplayHandler matchCompletedReplayHandler) {
        this.matchCompletedReplayHandler = matchCompletedReplayHandler;
    }

    @PostMapping("/matches")
    public ResponseEntity<Void> ingestMatch(@RequestBody MatchEvent event) {
        matchCompletedReplayHandler.enqueue(event);
        return ResponseEntity.accepted().build();
    }
}
