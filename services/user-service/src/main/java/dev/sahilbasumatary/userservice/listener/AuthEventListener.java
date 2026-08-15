package dev.sahilbasumatary.userservice.listener;

import dev.sahilbasumatary.common.event.OrganizationEvent;
import dev.sahilbasumatary.common.event.UserEvent;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.userservice.service.AuthProjectionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "tennisly.kafka.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuthEventListener {

    private final AuthProjectionService authProjectionService;

    public AuthEventListener(AuthProjectionService authProjectionService) {
        this.authProjectionService = authProjectionService;
    }

    @KafkaListener(topics = TopicNames.USER_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserEvent(UserEvent event) {
        authProjectionService.applyUser(event);
    }

    @KafkaListener(topics = TopicNames.ORGANIZATION_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrganizationEvent(OrganizationEvent event) {
        authProjectionService.applyOrganization(event);
    }
}
