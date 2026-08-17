package dev.sahilbasumatary.notificationservice.service;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.event.UserEvent;
import dev.sahilbasumatary.common.event.WebhookDomainEvent;
import dev.sahilbasumatary.common.event.WebhookEventTypes;
import dev.sahilbasumatary.common.notification.NotificationCategories;
import dev.sahilbasumatary.notificationservice.email.EmailDispatchService;
import dev.sahilbasumatary.notificationservice.email.EmailTemplateService;
import dev.sahilbasumatary.notificationservice.push.PushContentFactory;
import dev.sahilbasumatary.notificationservice.push.PushDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventHandler.class);

    private final EnqueueService enqueueService;
    private final EmailDispatchService emailDispatchService;
    private final EmailTemplateService emailTemplateService;
    private final PushDispatchService pushDispatchService;
    private final PushContentFactory pushContentFactory;

    public NotificationEventHandler(
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

    public void handleWebhook(WebhookDomainEvent event) {
        if (event == null) {
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

    public void handleMatch(MatchEvent event) {
        if (event == null) {
            return;
        }
        String webhookType = resolveWebhookType(event);
        if (webhookType == null) {
            return;
        }
        log.info(
                "Routing match event eventId={} type={} -> {}",
                event.getEventId(),
                event.getEventType(),
                webhookType);
        enqueueService.enqueue(webhookType, event);
    }

    public void handleUser(UserEvent event) {
        if (event == null) {
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

    private static String resolveWebhookType(MatchEvent event) {
        if (MatchEvent.MATCH_STATUS_CHANGED.equals(event.getEventType())
                && "COMPLETED".equals(event.getStatus())) {
            return WebhookEventTypes.MATCH_COMPLETED;
        }
        if (MatchEvent.MATCH_POINT_RECORDED.equals(event.getEventType())) {
            return WebhookEventTypes.MATCH_POINT_RECORDED;
        }
        return null;
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
