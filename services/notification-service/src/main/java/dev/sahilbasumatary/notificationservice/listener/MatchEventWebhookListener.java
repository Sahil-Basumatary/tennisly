package dev.sahilbasumatary.notificationservice.listener;

import dev.sahilbasumatary.common.event.BaseEvent;
import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.notificationservice.service.NotificationEventHandler;
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
public class MatchEventWebhookListener {

    private final NotificationEventHandler notificationEventHandler;

    public MatchEventWebhookListener(NotificationEventHandler notificationEventHandler) {
        this.notificationEventHandler = notificationEventHandler;
    }

    @KafkaListener(
            topics = TopicNames.MATCH_EVENTS,
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMatchEvent(BaseEvent baseEvent) {
        if (baseEvent instanceof MatchEvent event) {
            notificationEventHandler.handleMatch(event);
        }
    }
}
