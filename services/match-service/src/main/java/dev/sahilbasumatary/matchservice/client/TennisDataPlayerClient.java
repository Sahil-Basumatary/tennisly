package dev.sahilbasumatary.matchservice.client;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Resolves tennis-data player UUIDs by stable externalId. Failures stay soft for local boot. */
@Component
public class TennisDataPlayerClient {

    private static final Logger log = LoggerFactory.getLogger(TennisDataPlayerClient.class);

    private final RestClient restClient;

    public TennisDataPlayerClient(@Qualifier("tennisDataServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<UUID> findPlayerIdByExternalId(String externalId) {
        try {
            Map<?, ?> body =
                    restClient
                            .get()
                            .uri("/api/tennis/players/external/{externalId}", externalId)
                            .retrieve()
                            .body(Map.class);
            if (body == null || body.get("id") == null) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(String.valueOf(body.get("id"))));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            log.debug("tennis-data lookup failed for {}: {}", externalId, ex.getMessage());
            return Optional.empty();
        } catch (RestClientException | IllegalArgumentException ex) {
            log.debug("tennis-data unreachable for {}: {}", externalId, ex.getMessage());
            return Optional.empty();
        }
    }
}
