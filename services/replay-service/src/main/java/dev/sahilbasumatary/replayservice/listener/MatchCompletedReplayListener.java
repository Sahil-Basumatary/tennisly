package dev.sahilbasumatary.replayservice.listener;

import dev.sahilbasumatary.common.event.BaseEvent;
import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.replayservice.service.MatchCompletedReplayHandler;
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
public class MatchCompletedReplayListener {

    private final MatchCompletedReplayHandler matchCompletedReplayHandler;

    public MatchCompletedReplayListener(MatchCompletedReplayHandler matchCompletedReplayHandler) {
        this.matchCompletedReplayHandler = matchCompletedReplayHandler;
    }

    @KafkaListener(
            topics = TopicNames.MATCH_EVENTS,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMatchEvent(BaseEvent baseEvent) {
        if (baseEvent instanceof MatchEvent event) {
            matchCompletedReplayHandler.handle(event);
        }
    }
}
