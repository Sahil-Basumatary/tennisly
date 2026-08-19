package dev.sahilbasumatary.matchservice.client;

import dev.sahilbasumatary.common.event.MatchEvent;
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
public class ReplayEventClient {

    private static final Logger log = LoggerFactory.getLogger(ReplayEventClient.class);
    private final RestClient restClient;

    // Kafka is off on free Render; this HTTP path is how completed matches still materialize.

    public ReplayEventClient(
            @Value("${tennisly.clients.replay-service-uri:}") String replayServiceUri,
            @Value("${tennisly.internal-token:}") String internalToken) {
        if (replayServiceUri == null || replayServiceUri.isBlank()) {
            this.restClient = null;
            return;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(20));
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(replayServiceUri.replaceAll("/$", ""))
                        .requestFactory(factory);
        if (InternalToken.isEnabled(internalToken)) {
            builder.defaultHeader(InternalToken.HEADER, internalToken);
        }
        this.restClient = builder.build();
    }

    @Async
    public void relayMatch(MatchEvent event) {
        // Don't wake a sleeping replay JVM on every live point — only COMPLETED materializes.
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
        }
    }
}
