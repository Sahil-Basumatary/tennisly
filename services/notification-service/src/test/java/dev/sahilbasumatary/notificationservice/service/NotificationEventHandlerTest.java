package dev.sahilbasumatary.notificationservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.event.UserEvent;
import dev.sahilbasumatary.common.event.WebhookDomainEvent;
import dev.sahilbasumatary.common.event.WebhookEventTypes;
import dev.sahilbasumatary.common.notification.NotificationCategories;
import dev.sahilbasumatary.notificationservice.email.EmailDispatchService;
import dev.sahilbasumatary.notificationservice.email.EmailTemplateService;
import dev.sahilbasumatary.notificationservice.push.PushContentFactory;
import dev.sahilbasumatary.notificationservice.push.PushDispatchService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

    @Mock private EnqueueService enqueueService;
    @Mock private EmailDispatchService emailDispatchService;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock private PushDispatchService pushDispatchService;
    @Mock private PushContentFactory pushContentFactory;

    private NotificationEventHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new NotificationEventHandler(
                        enqueueService,
                        emailDispatchService,
                        emailTemplateService,
                        pushDispatchService,
                        pushContentFactory);
    }

    @Test
    void webhookTestEnqueuesForOrganization() {
        UUID orgId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        UUID endpointId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        WebhookDomainEvent event = WebhookDomainEvent.webhookTest(orgId, endpointId);
        handler.handleWebhook(event);
        verify(enqueueService).enqueue(WebhookEventTypes.WEBHOOK_TEST, event, orgId);
        verify(emailDispatchService, never()).dispatchForOrganization(any(), any(), any(), any());
    }

    @Test
    void apiKeyRevokedAlsoDispatchesOrgAlerts() {
        UUID orgId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        UUID keyId = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");
        WebhookDomainEvent event = WebhookDomainEvent.apiKeyRevoked(orgId, keyId, "tly_live_abcd");
        handler.handleWebhook(event);
        verify(enqueueService).enqueue(WebhookEventTypes.API_KEY_REVOKED, event, orgId);
        verify(emailDispatchService)
                .dispatchForOrganization(
                        eq(NotificationCategories.API_KEY_REVOKED),
                        eq(event.getEventId()),
                        eq(orgId),
                        any());
    }

    @Test
    void matchCompletedEnqueues() {
        MatchEvent event =
                MatchEvent.statusChanged(
                        UUID.fromString("cccccccc-dddd-eeee-ffff-000000000000"), "COMPLETED");
        handler.handleMatch(event);
        verify(enqueueService).enqueue(WebhookEventTypes.MATCH_COMPLETED, event);
    }

    @Test
    void matchCreatedDoesNotEnqueue() {
        MatchEvent event =
                MatchEvent.created(
                        UUID.fromString("cccccccc-dddd-eeee-ffff-000000000000"), "SCHEDULED");
        handler.handleMatch(event);
        verify(enqueueService, never()).enqueue(any(), any());
        verify(enqueueService, never()).enqueue(any(), any(), any());
    }

    @Test
    void userCreatedDispatchesWelcome() {
        UserEvent event = UserEvent.created("user_1", "a@b.c", "Ada", "Lovelace", null);
        handler.handleUser(event);
        verify(emailDispatchService)
                .dispatchForClerk(
                        eq(NotificationCategories.WELCOME),
                        eq(event.getEventId()),
                        eq("user_1"),
                        any());
    }

    @Test
    void userUpdatedDoesNotWelcome() {
        UserEvent event = UserEvent.updated("user_1", "a@b.c", "Ada", "Lovelace", null);
        handler.handleUser(event);
        verify(emailDispatchService, never()).dispatchForClerk(any(), any(), any(), any());
    }
}
