package dev.sahilbasumatary.analyticsservice.listener;

import dev.sahilbasumatary.analyticsservice.service.MatchAnalyticsIngestionService;
import dev.sahilbasumatary.common.event.BaseEvent;
import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.kafka.TopicNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!it")
@ConditionalOnProperty(
        name = "tennisly.kafka.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MatchEventAnalyticsListener {

    private static final Logger log = LoggerFactory.getLogger(MatchEventAnalyticsListener.class);

    private final MatchAnalyticsIngestionService ingestionService;

    public MatchEventAnalyticsListener(MatchAnalyticsIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @KafkaListener(
            topics = TopicNames.MATCH_EVENTS,
            groupId = "analytics-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMatchEvent(BaseEvent baseEvent) {
        if (!(baseEvent instanceof MatchEvent event)) {
            return;
        }
        if (MatchEvent.MATCH_STATUS_CHANGED.equals(event.getEventType())) {
            if (!"COMPLETED".equals(event.getStatus())) {
                return;
            }
        } else if (!MatchEvent.MATCH_POINT_RECORDED.equals(event.getEventType())) {
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
