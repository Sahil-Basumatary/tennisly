package dev.sahilbasumatary.replayservice.listener;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.replayservice.service.ReplayArtifactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * When a match flips to COMPLETED, materialize the deterministic replay into object storage so the
 * next GET is a cheap read. Failures retry then land on the match-events DLQ; match-service itself
 * never blocks on this path.
 */
@Component
public class MatchCompletedReplayListener {

    private static final Logger log = LoggerFactory.getLogger(MatchCompletedReplayListener.class);

    private final ReplayArtifactService replayArtifactService;

    public MatchCompletedReplayListener(ReplayArtifactService replayArtifactService) {
        this.replayArtifactService = replayArtifactService;
    }

    @KafkaListener(topics = TopicNames.MATCH_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void onMatchEvent(MatchEvent event) {
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
}
