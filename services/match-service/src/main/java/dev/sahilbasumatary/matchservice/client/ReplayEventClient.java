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
public class ReplayEventClient {

    private static final Logger log = LoggerFactory.getLogger(ReplayEventClient.class);
    private final RestClient restClient;

    public ReplayEventClient(
            @Value("${tennisly.clients.replay-service-uri:}") String replayServiceUri,
            @Value("${tennisly.internal-token:}") String internalToken) {
        this.restClient = PooledRestClients.maybeBuild(replayServiceUri, internalToken);
    }

    public void relayMatch(MatchEvent event) {
        if (restClient == null
                || event == null
                || !MatchEvent.MATCH_STATUS_CHANGED.equals(event.getEventType())
                || !"COMPLETED".equals(event.getStatus())) {
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
                    "Failed to relay match event to replay-service eventId={}: {}",
                    event.getEventId(),
                    ex.getMessage());
            throw ex;
        }
    }
}
