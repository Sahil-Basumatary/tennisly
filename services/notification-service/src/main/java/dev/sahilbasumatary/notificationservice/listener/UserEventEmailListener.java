package dev.sahilbasumatary.notificationservice.listener;

import dev.sahilbasumatary.common.event.BaseEvent;
import dev.sahilbasumatary.common.event.UserEvent;
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
public class UserEventEmailListener {

    private final NotificationEventHandler notificationEventHandler;

    public UserEventEmailListener(NotificationEventHandler notificationEventHandler) {
        this.notificationEventHandler = notificationEventHandler;
    }

    @KafkaListener(
            topics = TopicNames.USER_EVENTS,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onUserEvent(BaseEvent baseEvent) {
        if (baseEvent instanceof UserEvent event) {
            notificationEventHandler.handleUser(event);
        }
    }
}
