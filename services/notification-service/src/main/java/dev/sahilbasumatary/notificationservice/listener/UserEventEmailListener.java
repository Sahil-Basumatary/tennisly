package dev.sahilbasumatary.notificationservice.listener;

import dev.sahilbasumatary.common.event.BaseEvent;
import dev.sahilbasumatary.common.event.UserEvent;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.common.notification.NotificationCategories;
import dev.sahilbasumatary.notificationservice.email.EmailDispatchService;
import dev.sahilbasumatary.notificationservice.email.EmailTemplateService;
import dev.sahilbasumatary.notificationservice.push.PushContentFactory;
import dev.sahilbasumatary.notificationservice.push.PushDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventEmailListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventEmailListener.class);

    private final EmailDispatchService emailDispatchService;
    private final EmailTemplateService emailTemplateService;
    private final PushDispatchService pushDispatchService;
    private final PushContentFactory pushContentFactory;

    public UserEventEmailListener(
            EmailDispatchService emailDispatchService,
            EmailTemplateService emailTemplateService,
            PushDispatchService pushDispatchService,
            PushContentFactory pushContentFactory) {
        this.emailDispatchService = emailDispatchService;
        this.emailTemplateService = emailTemplateService;
        this.pushDispatchService = pushDispatchService;
        this.pushContentFactory = pushContentFactory;
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
        String displayName = buildDisplayName(event);
        if (event.getEmail() != null && !event.getEmail().isBlank()) {
            log.info(
                    "Dispatching welcome email clerkId={} eventId={}",
                    event.getClerkId(),
                    event.getEventId());
            emailDispatchService.dispatchForClerk(
                    NotificationCategories.WELCOME,
                    event.getEventId(),
                    event.getClerkId(),
                    () -> emailTemplateService.welcome(event.getEmail(), displayName));
        }
        pushDispatchService.dispatchForClerk(
                NotificationCategories.WELCOME,
                event.getEventId(),
                event.getClerkId(),
                () -> pushContentFactory.welcome(displayName));
    }

    private static String buildDisplayName(UserEvent event) {
        String first = event.getFirstName() == null ? "" : event.getFirstName().trim();
        String last = event.getLastName() == null ? "" : event.getLastName().trim();
        String combined = (first + " " + last).trim();
        if (!combined.isEmpty()) {
            return combined;
        }
        return event.getEmail() == null ? "there" : event.getEmail();
    }
}
