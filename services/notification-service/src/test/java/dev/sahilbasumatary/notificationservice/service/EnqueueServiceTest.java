package dev.sahilbasumatary.notificationservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.notificationservice.client.UserServiceWebhookClient;
import dev.sahilbasumatary.notificationservice.client.dto.WebhookSubscription;
import dev.sahilbasumatary.notificationservice.entity.WebhookDelivery;
import dev.sahilbasumatary.notificationservice.entity.WebhookDeliveryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EnqueueServiceTest {

    private final UserServiceWebhookClient webhookClient = mock(UserServiceWebhookClient.class);
    private final WebhookDeliveryRepository deliveryRepository =
            mock(WebhookDeliveryRepository.class);
    private final WebhookPayloadBuilder payloadBuilder = mock(WebhookPayloadBuilder.class);
    private final EnqueueService enqueueService =
            new EnqueueService(webhookClient, deliveryRepository, payloadBuilder);

    @Test
    void skipsWhenNoSubscriptions() {
        when(webhookClient.getSubscriptions("match.completed", null)).thenReturn(List.of());
        MatchEvent event = MatchEvent.created(UUID.randomUUID(), "COMPLETED");
        event.setEventId("evt-1");
        enqueueService.enqueue("match.completed", event);
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void skipsDuplicateIdempotencyKey() {
        UUID endpointId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(webhookClient.getSubscriptions("match.completed", null))
                .thenReturn(
                        List.of(
                                new WebhookSubscription(
                                        endpointId, orgId, "https://hooks.example/h", "sec")));
        when(payloadBuilder.buildEnvelope(eq("match.completed"), any())).thenReturn("{}");
        when(deliveryRepository.existsByIdempotencyKey(endpointId + ":evt-1")).thenReturn(true);
        MatchEvent event = MatchEvent.created(UUID.randomUUID(), "COMPLETED");
        event.setEventId("evt-1");
        enqueueService.enqueue("match.completed", event);
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void savesPendingDelivery() {
        UUID endpointId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(webhookClient.getSubscriptions("match.completed", null))
                .thenReturn(
                        List.of(
                                new WebhookSubscription(
                                        endpointId, orgId, "https://hooks.example/h", "sec")));
        when(payloadBuilder.buildEnvelope(eq("match.completed"), any())).thenReturn("{\"ok\":true}");
        when(deliveryRepository.existsByIdempotencyKey(endpointId + ":evt-1")).thenReturn(false);
        MatchEvent event = MatchEvent.created(UUID.randomUUID(), "COMPLETED");
        event.setEventId("evt-1");
        enqueueService.enqueue("match.completed", event);
        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        WebhookDelivery saved = captor.getValue();
        assertEquals(endpointId, saved.getEndpointId());
        assertEquals(orgId, saved.getOrganizationId());
        assertEquals("match.completed", saved.getEventType());
        assertEquals("{\"ok\":true}", saved.getPayload());
        assertEquals(endpointId + ":evt-1", saved.getIdempotencyKey());
    }
}
