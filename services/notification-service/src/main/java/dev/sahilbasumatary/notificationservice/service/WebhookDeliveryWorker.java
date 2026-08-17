package dev.sahilbasumatary.notificationservice.service;

import dev.sahilbasumatary.common.notification.NotificationCategories;
import dev.sahilbasumatary.common.webhook.WebhookSignature;
import dev.sahilbasumatary.notificationservice.client.UserServiceWebhookClient;
import dev.sahilbasumatary.notificationservice.client.dto.WebhookSubscription;
import dev.sahilbasumatary.notificationservice.email.EmailDispatchService;
import dev.sahilbasumatary.notificationservice.email.EmailTemplateService;
import dev.sahilbasumatary.notificationservice.entity.DeliveryStatus;
import dev.sahilbasumatary.notificationservice.entity.WebhookDelivery;
import dev.sahilbasumatary.notificationservice.entity.WebhookDeliveryRepository;
import dev.sahilbasumatary.notificationservice.push.PushContentFactory;
import dev.sahilbasumatary.notificationservice.push.PushDispatchService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebhookDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryWorker.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebhookDeliveryRepository deliveryRepository;
    private final UserServiceWebhookClient webhookClient;
    private final EmailDispatchService emailDispatchService;
    private final EmailTemplateService emailTemplateService;
    private final PushDispatchService pushDispatchService;
    private final PushContentFactory pushContentFactory;
    private final HttpClient httpClient;

    @Value("${notification.delivery.batch-size:50}")
    private int batchSize;

    // Two constructors: Spring will not guess and then demands a no-arg ctor.
    @Autowired
    public WebhookDeliveryWorker(
            WebhookDeliveryRepository deliveryRepository,
            UserServiceWebhookClient webhookClient,
            EmailDispatchService emailDispatchService,
            EmailTemplateService emailTemplateService,
            PushDispatchService pushDispatchService,
            PushContentFactory pushContentFactory) {
        this(
                deliveryRepository,
                webhookClient,
                emailDispatchService,
                emailTemplateService,
                pushDispatchService,
                pushContentFactory,
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
    }

    WebhookDeliveryWorker(
            WebhookDeliveryRepository deliveryRepository,
            UserServiceWebhookClient webhookClient,
            EmailDispatchService emailDispatchService,
            EmailTemplateService emailTemplateService,
            PushDispatchService pushDispatchService,
            PushContentFactory pushContentFactory,
            HttpClient httpClient) {
        this.deliveryRepository = deliveryRepository;
        this.webhookClient = webhookClient;
        this.emailDispatchService = emailDispatchService;
        this.emailTemplateService = emailTemplateService;
        this.pushDispatchService = pushDispatchService;
        this.pushContentFactory = pushContentFactory;
        this.httpClient = httpClient;
    }

    @Scheduled(fixedDelayString = "${notification.delivery.worker-delay-ms:2000}")
    public void poll() {
        List<WebhookDelivery> batch = deliveryRepository.findDueDeliveries(Instant.now(), batchSize);
        for (WebhookDelivery delivery : batch) {
            attemptDelivery(delivery);
        }
    }

    private void attemptDelivery(WebhookDelivery delivery) {
        WebhookSubscription endpoint;
        try {
            endpoint = webhookClient.getEndpoint(delivery.getEndpointId());
        } catch (Exception ex) {
            log.error("Cannot fetch endpoint={} for delivery={}: {}",
                    delivery.getEndpointId(), delivery.getId(), ex.getMessage());
            markFailed(delivery, null, ex.getMessage(), null);
            return;
        }
        if (endpoint == null) {
            log.warn("Endpoint {} not found, marking delivery {} as DEAD",
                    delivery.getEndpointId(), delivery.getId());
            delivery.setStatus(DeliveryStatus.DEAD);
            delivery.setLastError("Endpoint not found");
            deliveryRepository.save(delivery);
            return;
        }
        String signature = WebhookSignature.sign(endpoint.signingSecret(), delivery.getPayload());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.targetUrl()))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("X-Tennisly-Event", delivery.getEventType())
                .header("X-Tennisly-Delivery", delivery.getId().toString())
                .header("X-Tennisly-Signature", signature)
                .header("User-Agent", "Tennisly-Webhooks/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(delivery.getPayload()))
                .build();
        long startMs = System.currentTimeMillis();
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int elapsed = (int) (System.currentTimeMillis() - startMs);
            delivery.setResponseMs(elapsed);
            delivery.setLastHttpStatus(response.statusCode());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                delivery.setStatus(DeliveryStatus.SUCCESS);
                delivery.setDeliveredAt(Instant.now());
                delivery.setAttemptCount(delivery.getAttemptCount() + 1);
                deliveryRepository.save(delivery);
                log.info("Webhook delivered delivery={} endpoint={} status={} ms={}",
                        delivery.getId(), delivery.getEndpointId(),
                        response.statusCode(), elapsed);
                try {
                    webhookClient.markDelivered(delivery.getEndpointId());
                } catch (Exception ex) {
                    log.debug("mark-delivered call failed for endpoint={}: {}",
                            delivery.getEndpointId(), ex.getMessage());
                }
            } else {
                markFailed(delivery, response.statusCode(),
                        "HTTP " + response.statusCode(), elapsed);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            markFailed(delivery, null, "Interrupted: " + ex.getMessage(), null);
        } catch (Exception ex) {
            int elapsed = (int) (System.currentTimeMillis() - startMs);
            markFailed(delivery, null, ex.getMessage(), elapsed);
        }
    }

    private void markFailed(
            WebhookDelivery delivery, Integer httpStatus, String error, Integer responseMs) {
        int nextAttempt = delivery.getAttemptCount() + 1;
        delivery.setAttemptCount(nextAttempt);
        delivery.setLastError(error);
        if (httpStatus != null) {
            delivery.setLastHttpStatus(httpStatus);
        }
        if (responseMs != null) {
            delivery.setResponseMs(responseMs);
        }
        if (nextAttempt >= delivery.getMaxAttempts()) {
            delivery.setStatus(DeliveryStatus.DEAD);
            log.warn("Delivery {} exhausted retries, marking DEAD", delivery.getId());
            deliveryRepository.save(delivery);
            notifyWebhookFailed(delivery);
            return;
        }
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setNextAttemptAt(BackoffCalculator.nextAttemptAt(nextAttempt));
        deliveryRepository.save(delivery);
    }

    private void notifyWebhookFailed(WebhookDelivery delivery) {
        try {
            emailDispatchService.dispatchForOrganization(
                    NotificationCategories.WEBHOOK_FAILED,
                    delivery.getId().toString(),
                    delivery.getOrganizationId(),
                    recipient ->
                            emailTemplateService.webhookFailed(
                                    recipient.email(),
                                    recipient.displayName(),
                                    delivery.getEventType(),
                                    delivery.getLastError()));
            pushDispatchService.dispatchForOrganization(
                    NotificationCategories.WEBHOOK_FAILED,
                    delivery.getId().toString(),
                    delivery.getOrganizationId(),
                    recipient -> pushContentFactory.webhookFailed(delivery.getEventType()));
        } catch (Exception ex) {
            log.warn(
                    "Failed to dispatch webhook-failed alerts for delivery={}: {}",
                    delivery.getId(),
                    ex.getMessage());
        }
    }
}
