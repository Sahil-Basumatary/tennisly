package dev.sahilbasumatary.analyticsservice.listener;

import dev.sahilbasumatary.analyticsservice.service.MatchEventAnalyticsHandler;
import dev.sahilbasumatary.common.event.BaseEvent;
import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.kafka.TopicNames;
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

    private final MatchEventAnalyticsHandler matchEventAnalyticsHandler;

    public MatchEventAnalyticsListener(MatchEventAnalyticsHandler matchEventAnalyticsHandler) {
        this.matchEventAnalyticsHandler = matchEventAnalyticsHandler;
    }

    @KafkaListener(
            topics = TopicNames.MATCH_EVENTS,
            groupId = "analytics-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMatchEvent(BaseEvent baseEvent) {
        if (baseEvent instanceof MatchEvent event) {
            matchEventAnalyticsHandler.handle(event);
        }
    }
}
