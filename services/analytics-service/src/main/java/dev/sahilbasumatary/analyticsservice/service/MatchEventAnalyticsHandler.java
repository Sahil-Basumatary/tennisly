package dev.sahilbasumatary.analyticsservice.service;

import dev.sahilbasumatary.common.event.MatchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MatchEventAnalyticsHandler {

    private static final Logger log = LoggerFactory.getLogger(MatchEventAnalyticsHandler.class);

    private final MatchAnalyticsIngestionService ingestionService;

    public MatchEventAnalyticsHandler(MatchAnalyticsIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public void handle(MatchEvent event) {
        if (event == null || event.getMatchId() == null) {
            return;
        }
        if (MatchEvent.MATCH_STATUS_CHANGED.equals(event.getEventType())) {
            if (!"COMPLETED".equals(event.getStatus())) {
                return;
            }
        } else if (!MatchEvent.MATCH_ARCHIVE_COMPLETED.equals(event.getEventType())
                && !MatchEvent.MATCH_POINT_RECORDED.equals(event.getEventType())) {
            return;
        }
        try {
            boolean processed = ingestionService.processEvent(event);
            if (processed) {
                log.info(
                        "Processed analytics event type={} matchId={} eventId={}",
                        event.getEventType(),
                        event.getMatchId(),
                        event.getEventId());
            }
        } catch (RuntimeException ex) {
            log.error(
                    "Analytics ingestion failed type={} matchId={} eventId={}: {}",
                    event.getEventType(),
                    event.getMatchId(),
                    event.getEventId(),
                    ex.getMessage());
            throw ex;
        }
    }
}
