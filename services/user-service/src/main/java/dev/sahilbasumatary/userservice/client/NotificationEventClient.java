package dev.sahilbasumatary.userservice.client;

import dev.sahilbasumatary.common.event.UserEvent;
import dev.sahilbasumatary.common.event.WebhookDomainEvent;
import dev.sahilbasumatary.common.security.InternalToken;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NotificationEventClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventClient.class);
    private final RestClient restClient;

    // Kafka is off on free Render; this HTTP path is how events still enqueue.

    public NotificationEventClient(
            @Value("${tennisly.clients.notification-service-uri:}") String notificationServiceUri,
            @Value("${tennisly.internal-token:}") String internalToken) {
        if (notificationServiceUri == null || notificationServiceUri.isBlank()) {
            this.restClient = null;
            return;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(20));
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(notificationServiceUri.replaceAll("/$", ""))
                        .requestFactory(factory);
        if (InternalToken.isEnabled(internalToken)) {
            builder.defaultHeader(InternalToken.HEADER, internalToken);
        }
        this.restClient = builder.build();
    }

    @Async
    public void relayWebhook(WebhookDomainEvent event) {
        post("/internal/events/webhooks", event, event.getEventId());
    }

    @Async
    public void relayUser(UserEvent event) {
        post("/internal/events/users", event, event.getClerkId());
    }

    private void post(String path, Object body, String key) {
        if (restClient == null) {
            return;
        }
        try {
            restClient
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Failed to relay event to notification-service key={}: {}", key, ex.getMessage());
        }
    }
}
