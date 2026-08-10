package dev.sahilbasumatary.notificationservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.notificationservice.client.UserServiceWebhookClient;
import dev.sahilbasumatary.notificationservice.client.dto.WebhookSubscription;
import dev.sahilbasumatary.notificationservice.email.EmailDispatchService;
import dev.sahilbasumatary.notificationservice.email.EmailTemplateService;
import dev.sahilbasumatary.notificationservice.entity.DeliveryStatus;
import dev.sahilbasumatary.notificationservice.entity.WebhookDelivery;
import dev.sahilbasumatary.notificationservice.entity.WebhookDeliveryRepository;
import dev.sahilbasumatary.notificationservice.push.PushContentFactory;
import dev.sahilbasumatary.notificationservice.push.PushDispatchService;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class WebhookDeliveryWorkerTest {

    private MockWebServer webhookTarget;
    private WebhookDeliveryRepository deliveryRepository;
    private UserServiceWebhookClient webhookClient;
    private EmailDispatchService emailDispatchService;
    private EmailTemplateService emailTemplateService;
    private PushDispatchService pushDispatchService;
    private PushContentFactory pushContentFactory;
    private WebhookDeliveryWorker worker;

    @BeforeEach
    void setUp() throws IOException {
        webhookTarget = new MockWebServer();
        webhookTarget.start();
        deliveryRepository = mock(WebhookDeliveryRepository.class);
        webhookClient = mock(UserServiceWebhookClient.class);
        emailDispatchService = mock(EmailDispatchService.class);
        emailTemplateService = mock(EmailTemplateService.class);
        pushDispatchService = mock(PushDispatchService.class);
        pushContentFactory = mock(PushContentFactory.class);
        worker =
                new WebhookDeliveryWorker(
                        deliveryRepository,
                        webhookClient,
                        emailDispatchService,
                        emailTemplateService,
                        pushDispatchService,
                        pushContentFactory,
                        HttpClient.newHttpClient());
        ReflectionTestUtils.setField(worker, "batchSize", 10);
    }

    @AfterEach
    void tearDown() throws IOException {
        webhookTarget.shutdown();
    }

    @Test
    void marksSuccessAndCallsMarkDelivered() {
        WebhookDelivery delivery = pendingDelivery();
        when(deliveryRepository.findDueDeliveries(any(Instant.class), eq(10)))
                .thenReturn(List.of(delivery));
        when(webhookClient.getEndpoint(delivery.getEndpointId()))
                .thenReturn(
                        new WebhookSubscription(
                                delivery.getEndpointId(),
                                delivery.getOrganizationId(),
                                webhookTarget.url("/hook").toString(),
                                "whsec_testsecret"));
        webhookTarget.enqueue(new MockResponse().setResponseCode(204));
        worker.poll();
        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertEquals(DeliveryStatus.SUCCESS, captor.getValue().getStatus());
        verify(webhookClient).markDelivered(delivery.getEndpointId());
    }

    @Test
    void marksFailedOnHttpError() {
        WebhookDelivery delivery = pendingDelivery();
        when(deliveryRepository.findDueDeliveries(any(Instant.class), eq(10)))
                .thenReturn(List.of(delivery));
        when(webhookClient.getEndpoint(delivery.getEndpointId()))
                .thenReturn(
                        new WebhookSubscription(
                                delivery.getEndpointId(),
                                delivery.getOrganizationId(),
                                webhookTarget.url("/hook").toString(),
                                "whsec_testsecret"));
        webhookTarget.enqueue(new MockResponse().setResponseCode(500));
        worker.poll();
        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertEquals(DeliveryStatus.FAILED, captor.getValue().getStatus());
        verify(webhookClient, never()).markDelivered(any());
    }

    @Test
    void marksDeadWhenEndpointMissing() {
        WebhookDelivery delivery = pendingDelivery();
        when(deliveryRepository.findDueDeliveries(any(Instant.class), eq(10)))
                .thenReturn(List.of(delivery));
        when(webhookClient.getEndpoint(delivery.getEndpointId())).thenReturn(null);
        worker.poll();
        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertEquals(DeliveryStatus.DEAD, captor.getValue().getStatus());
    }

    @Test
    void marksDeadAndNotifiesWhenRetriesExhausted() {
        WebhookDelivery delivery = pendingDelivery();
        delivery.setAttemptCount(4);
        delivery.setMaxAttempts(5);
        when(deliveryRepository.findDueDeliveries(any(Instant.class), eq(10)))
                .thenReturn(List.of(delivery));
        when(webhookClient.getEndpoint(delivery.getEndpointId()))
                .thenReturn(
                        new WebhookSubscription(
                                delivery.getEndpointId(),
                                delivery.getOrganizationId(),
                                webhookTarget.url("/hook").toString(),
                                "whsec_testsecret"));
        webhookTarget.enqueue(new MockResponse().setResponseCode(503));
        worker.poll();
        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertEquals(DeliveryStatus.DEAD, captor.getValue().getStatus());
        verify(emailDispatchService)
                .dispatchForOrganization(anyString(), anyString(), any(UUID.class), any());
        verify(pushDispatchService)
                .dispatchForOrganization(anyString(), anyString(), any(UUID.class), any());
    }

    @Test
    void marksFailedWhenEndpointLookupThrows() {
        WebhookDelivery delivery = pendingDelivery();
        when(deliveryRepository.findDueDeliveries(any(Instant.class), eq(10)))
                .thenReturn(List.of(delivery));
        when(webhookClient.getEndpoint(delivery.getEndpointId()))
                .thenThrow(new RuntimeException("user-service down"));
        worker.poll();
        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertEquals(DeliveryStatus.FAILED, captor.getValue().getStatus());
        verify(emailDispatchService, never())
                .dispatchForOrganization(anyString(), anyString(), any(), any());
    }

    private static WebhookDelivery pendingDelivery() {
        WebhookDelivery delivery = new WebhookDelivery();
        ReflectionTestUtils.setField(delivery, "id", UUID.randomUUID());
        delivery.setEndpointId(UUID.randomUUID());
        delivery.setOrganizationId(UUID.randomUUID());
        delivery.setEventId("evt-1");
        delivery.setEventType("match.completed");
        delivery.setPayload("{\"hello\":\"world\"}");
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttemptCount(0);
        delivery.setMaxAttempts(5);
        delivery.setNextAttemptAt(Instant.now());
        delivery.setIdempotencyKey(UUID.randomUUID() + ":evt-1");
        return delivery;
    }
}
