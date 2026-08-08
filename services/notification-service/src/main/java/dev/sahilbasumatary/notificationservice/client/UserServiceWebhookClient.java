package dev.sahilbasumatary.notificationservice.client;

import dev.sahilbasumatary.notificationservice.client.dto.WebhookSubscription;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class UserServiceWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceWebhookClient.class);

    private final RestClient restClient;

    public UserServiceWebhookClient(@Qualifier("userServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<WebhookSubscription> getSubscriptions(String eventType) {
        return getSubscriptions(eventType, null);
    }

    public List<WebhookSubscription> getSubscriptions(String eventType, UUID organizationId) {
        if (organizationId == null) {
            return restClient
                    .get()
                    .uri("/internal/webhooks/subscriptions?eventType={eventType}", eventType)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        }
        return restClient
                .get()
                .uri(
                        "/internal/webhooks/subscriptions?eventType={eventType}&organizationId={organizationId}",
                        eventType,
                        organizationId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public WebhookSubscription getEndpoint(UUID endpointId) {
        return restClient
                .get()
                .uri("/internal/webhooks/endpoints/{id}", endpointId)
                .retrieve()
                .body(WebhookSubscription.class);
    }

    public void markDelivered(UUID endpointId) {
        try {
            restClient
                    .post()
                    .uri("/internal/webhooks/endpoints/{id}/mark-delivered", endpointId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Failed to mark endpoint {} as delivered: {}", endpointId, ex.getMessage());
        }
    }
}
