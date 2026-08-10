package dev.sahilbasumatary.notificationservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.event.WebhookEventTypes;
import dev.sahilbasumatary.common.webhook.WebhookSignature;
import dev.sahilbasumatary.notificationservice.client.UserServiceWebhookClient;
import dev.sahilbasumatary.notificationservice.client.dto.WebhookSubscription;
import dev.sahilbasumatary.notificationservice.entity.DeliveryStatus;
import dev.sahilbasumatary.notificationservice.entity.WebhookDelivery;
import dev.sahilbasumatary.notificationservice.entity.WebhookDeliveryRepository;
import dev.sahilbasumatary.notificationservice.service.EnqueueService;
import dev.sahilbasumatary.notificationservice.service.WebhookDeliveryWorker;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("it")
@Testcontainers(disabledWithoutDocker = true)
class WebhookDeliveryWorkerIT {

    private static final String SIGNING_SECRET = "whsec_it_test_secret_value";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("tennisly_notifications_it")
                    .withUsername("tennisly")
                    .withPassword("tennisly_dev");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private EnqueueService enqueueService;

    @Autowired
    private WebhookDeliveryWorker deliveryWorker;

    @Autowired
    private WebhookDeliveryRepository deliveryRepository;

    @MockBean
    private UserServiceWebhookClient webhookClient;

    private HttpServer httpServer;
    private AtomicInteger responseStatus;
    private AtomicReference<String> capturedSignature;
    private AtomicReference<String> capturedBody;
    private UUID endpointId;
    private UUID organizationId;

    @BeforeEach
    void setUp() throws IOException {
        deliveryRepository.deleteAll();
        reset(webhookClient);
        endpointId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        responseStatus = new AtomicInteger(200);
        capturedSignature = new AtomicReference<>();
        capturedBody = new AtomicReference<>();
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext(
                "/hooks",
                exchange -> {
                    capturedSignature.set(
                            exchange.getRequestHeaders().getFirst("X-Tennisly-Signature"));
                    capturedBody.set(
                            new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    exchange.sendResponseHeaders(responseStatus.get(), -1);
                    exchange.close();
                });
        httpServer.start();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void enqueueCreatesPendingOutboxRow() {
        stubSubscriptions();
        MatchEvent event = MatchEvent.statusChanged(UUID.randomUUID(), "COMPLETED");
        enqueueService.enqueue(WebhookEventTypes.MATCH_COMPLETED, event);
        List<WebhookDelivery> rows = deliveryRepository.findAll();
        assertEquals(1, rows.size());
        WebhookDelivery row = rows.get(0);
        assertEquals(DeliveryStatus.PENDING, row.getStatus());
        assertEquals(endpointId, row.getEndpointId());
        assertEquals(organizationId, row.getOrganizationId());
        assertEquals(WebhookEventTypes.MATCH_COMPLETED, row.getEventType());
        assertEquals(endpointId + ":" + event.getEventId(), row.getIdempotencyKey());
        assertTrue(row.getPayload().contains(WebhookEventTypes.MATCH_COMPLETED));
    }

    @Test
    void workerMarksSuccessOnHttp200AndSendsValidSignature() {
        stubSubscriptions();
        stubEndpoint();
        MatchEvent event = MatchEvent.statusChanged(UUID.randomUUID(), "COMPLETED");
        enqueueService.enqueue(WebhookEventTypes.MATCH_COMPLETED, event);
        deliveryWorker.poll();
        WebhookDelivery row = deliveryRepository.findAll().get(0);
        assertEquals(DeliveryStatus.SUCCESS, row.getStatus());
        assertEquals(200, row.getLastHttpStatus());
        assertNotNull(row.getDeliveredAt());
        assertEquals(1, row.getAttemptCount());
        assertNotNull(capturedSignature.get());
        assertNotNull(capturedBody.get());
        assertTrue(
                WebhookSignature.verify(
                        SIGNING_SECRET,
                        capturedSignature.get(),
                        capturedBody.get().getBytes(StandardCharsets.UTF_8),
                        Instant.now().getEpochSecond()));
        verify(webhookClient).markDelivered(endpointId);
    }

    @Test
    void workerSchedulesRetryOnHttp500() {
        responseStatus.set(500);
        stubSubscriptions();
        stubEndpoint();
        MatchEvent event = MatchEvent.statusChanged(UUID.randomUUID(), "COMPLETED");
        enqueueService.enqueue(WebhookEventTypes.MATCH_COMPLETED, event);
        final Instant before = Instant.now();
        deliveryWorker.poll();
        WebhookDelivery row = deliveryRepository.findAll().get(0);
        assertEquals(DeliveryStatus.FAILED, row.getStatus());
        assertEquals(500, row.getLastHttpStatus());
        assertEquals(1, row.getAttemptCount());
        assertNotNull(row.getNextAttemptAt());
        assertTrue(row.getNextAttemptAt().isAfter(before.plus(Duration.ofSeconds(90))));
        assertTrue(row.getNextAttemptAt().isBefore(before.plus(Duration.ofMinutes(3))));
        assertFalse(
                deliveryRepository
                        .findDueDeliveries(Instant.now(), 10)
                        .stream()
                        .anyMatch(d -> d.getId().equals(row.getId())));
    }

    private void stubSubscriptions() {
        when(webhookClient.getSubscriptions(eq(WebhookEventTypes.MATCH_COMPLETED), any()))
                .thenReturn(List.of(subscription()));
    }

    private void stubEndpoint() {
        when(webhookClient.getEndpoint(endpointId)).thenReturn(subscription());
    }

    private WebhookSubscription subscription() {
        return new WebhookSubscription(
                endpointId, organizationId, receiverUrl(), SIGNING_SECRET);
    }

    private String receiverUrl() {
        return "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/hooks";
    }
}
