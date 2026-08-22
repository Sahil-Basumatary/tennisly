package dev.sahilbasumatary.matchservice.client;

import dev.sahilbasumatary.common.event.MatchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NotificationEventClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventClient.class);
    private final RestClient restClient;

    public NotificationEventClient(
            @Value("${tennisly.clients.notification-service-uri:}") String notificationServiceUri,
            @Value("${tennisly.internal-token:}") String internalToken) {
        this.restClient = PooledRestClients.maybeBuild(notificationServiceUri, internalToken);
    }

    public void relayMatch(MatchEvent event) {
        if (restClient == null) {
            return;
        }
        try {
            restClient
                    .post()
                    .uri("/internal/events/matches")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error(
                    "Failed to relay match event to notification-service eventId={}: {}",
                    event.getEventId(),
                    ex.getMessage());
            throw ex;
        }
    }
}
