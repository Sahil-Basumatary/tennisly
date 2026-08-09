package dev.sahilbasumatary.notificationservice.listener;

import dev.sahilbasumatary.common.event.BaseEvent;
import dev.sahilbasumatary.common.event.WebhookDomainEvent;
import dev.sahilbasumatary.common.event.WebhookEventTypes;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.common.notification.NotificationCategories;
import dev.sahilbasumatary.notificationservice.email.EmailDispatchService;
import dev.sahilbasumatary.notificationservice.email.EmailTemplateService;
import dev.sahilbasumatary.notificationservice.push.PushContentFactory;
import dev.sahilbasumatary.notificationservice.push.PushDispatchService;
import dev.sahilbasumatary.notificationservice.service.EnqueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!it")
public class WebhookDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebhookDomainEventListener.class);

    private final EnqueueService enqueueService;
    private final EmailDispatchService emailDispatchService;
    private final EmailTemplateService emailTemplateService;
    private final PushDispatchService pushDispatchService;
    private final PushContentFactory pushContentFactory;

    public WebhookDomainEventListener(
            EnqueueService enqueueService,
            EmailDispatchService emailDispatchService,
            EmailTemplateService emailTemplateService,
            PushDispatchService pushDispatchService,
            PushContentFactory pushContentFactory) {
        this.enqueueService = enqueueService;
        this.emailDispatchService = emailDispatchService;
        this.emailTemplateService = emailTemplateService;
        this.pushDispatchService = pushDispatchService;
        this.pushContentFactory = pushContentFactory;
    }

    @KafkaListener(
            topics = TopicNames.WEBHOOK_EVENTS,
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onWebhookDomainEvent(BaseEvent baseEvent) {
        if (!(baseEvent instanceof WebhookDomainEvent event)) {
            return;
        }
        String webhookType = event.getPublicEventType();
        if (webhookType == null || webhookType.isBlank()) {
            log.warn(
                    "WebhookDomainEvent missing publicEventType, eventId={}", event.getEventId());
            return;
        }
        log.info(
                "Routing webhook domain event eventId={} type={} orgId={}",
                event.getEventId(),
                webhookType,
                event.getOrganizationId());
        enqueueService.enqueue(webhookType, event, event.getOrganizationId());
        if (WebhookEventTypes.API_KEY_REVOKED.equals(webhookType)
                && event.getOrganizationId() != null) {
            Object keyPrefix = event.getData() == null ? null : event.getData().get("keyPrefix");
            String prefix = keyPrefix == null ? "tly_live_" : String.valueOf(keyPrefix);
            emailDispatchService.dispatchForOrganization(
                    NotificationCategories.API_KEY_REVOKED,
                    event.getEventId(),
                    event.getOrganizationId(),
                    recipient ->
                            emailTemplateService.apiKeyRevoked(
                                    recipient.email(), recipient.displayName(), prefix));
            pushDispatchService.dispatchForOrganization(
                    NotificationCategories.API_KEY_REVOKED,
                    event.getEventId(),
                    event.getOrganizationId(),
                    recipient -> pushContentFactory.apiKeyRevoked(prefix));
        }
    }
}
