package dev.sahilbasumatary.notificationservice.listener;

import dev.sahilbasumatary.common.event.BaseEvent;
import dev.sahilbasumatary.common.event.UserEvent;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.common.notification.EmailCategories;
import dev.sahilbasumatary.notificationservice.email.EmailDispatchService;
import dev.sahilbasumatary.notificationservice.email.EmailTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventEmailListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventEmailListener.class);

    private final EmailDispatchService emailDispatchService;
    private final EmailTemplateService emailTemplateService;

    public UserEventEmailListener(
            EmailDispatchService emailDispatchService, EmailTemplateService emailTemplateService) {
        this.emailDispatchService = emailDispatchService;
        this.emailTemplateService = emailTemplateService;
    }

    @KafkaListener(
            topics = TopicNames.USER_EVENTS,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onUserEvent(BaseEvent baseEvent) {
        if (!(baseEvent instanceof UserEvent event)) {
            return;
        }
        if (!"USER_CREATED".equals(event.getEventType())) {
            return;
        }
        if (event.getClerkId() == null || event.getClerkId().isBlank()) {
            return;
        }
        if (event.getEmail() == null || event.getEmail().isBlank()) {
            log.info(
                    "Skipping welcome email — no email on USER_CREATED clerkId={}",
                    event.getClerkId());
            return;
        }
        String displayName = buildDisplayName(event);
        log.info(
                "Dispatching welcome email clerkId={} eventId={}",
                event.getClerkId(),
                event.getEventId());
        emailDispatchService.dispatchForClerk(
                EmailCategories.WELCOME,
                event.getEventId(),
                event.getClerkId(),
                () -> emailTemplateService.welcome(event.getEmail(), displayName));
    }

    private static String buildDisplayName(UserEvent event) {
        String first = event.getFirstName() == null ? "" : event.getFirstName().trim();
        String last = event.getLastName() == null ? "" : event.getLastName().trim();
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? event.getEmail() : combined;
    }
}
