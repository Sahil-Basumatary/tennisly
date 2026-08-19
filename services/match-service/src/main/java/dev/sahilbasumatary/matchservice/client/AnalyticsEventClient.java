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
public class AnalyticsEventClient {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventClient.class);
    private final RestClient restClient;

    // Kafka is off on free Render; this HTTP path is how match tape still indexes.

    public AnalyticsEventClient(
            @Value("${tennisly.clients.analytics-service-uri:}") String analyticsServiceUri,
            @Value("${tennisly.internal-token:}") String internalToken) {
        if (analyticsServiceUri == null || analyticsServiceUri.isBlank()) {
            this.restClient = null;
            return;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(20));
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(analyticsServiceUri.replaceAll("/$", ""))
                        .requestFactory(factory);
        if (InternalToken.isEnabled(internalToken)) {
            builder.defaultHeader(InternalToken.HEADER, internalToken);
        }
        this.restClient = builder.build();
    }

    @Async
    public void relayMatch(MatchEvent event) {
        post(event);
    }

    private void post(MatchEvent event) {
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
                    "Failed to relay match event to analytics-service eventId={}: {}",
                    event.getEventId(),
                    ex.getMessage());
        }
    }
}
