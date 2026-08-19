package dev.sahilbasumatary.replayservice.service;

import dev.sahilbasumatary.common.event.MatchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MatchCompletedReplayHandler {

    private static final Logger log = LoggerFactory.getLogger(MatchCompletedReplayHandler.class);

    private final ReplayArtifactService replayArtifactService;

    public MatchCompletedReplayHandler(ReplayArtifactService replayArtifactService) {
        this.replayArtifactService = replayArtifactService;
    }

    public void handle(MatchEvent event) {
        if (event == null || event.getMatchId() == null) {
            return;
        }
        if (!MatchEvent.MATCH_STATUS_CHANGED.equals(event.getEventType())) {
            return;
        }
        if (!"COMPLETED".equals(event.getStatus())) {
            return;
        }
        log.info(
                "Match completed — materializing replay matchId={} eventId={}",
                event.getMatchId(),
                event.getEventId());
        replayArtifactService.materialize(event.getMatchId());
    }

    // HTTP ingest must 202 before physics + match-service fetch; Kafka still calls handle() so DLQ works.
    @Async("replayTaskExecutor")
    public void enqueue(MatchEvent event) {
        try {
            handle(event);
        } catch (RuntimeException ex) {
            log.error(
                    "Replay materialize failed matchId={} eventId={}: {}",
                    event != null ? event.getMatchId() : null,
                    event != null ? event.getEventId() : null,
                    ex.getMessage());
        }
    }
}
