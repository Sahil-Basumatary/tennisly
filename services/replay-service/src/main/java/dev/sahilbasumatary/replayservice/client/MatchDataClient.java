package dev.sahilbasumatary.replayservice.client;

import dev.sahilbasumatary.replayservice.client.dto.MatchPointSummary;
import dev.sahilbasumatary.replayservice.client.dto.MatchSummary;
import dev.sahilbasumatary.replayservice.exception.DownstreamServiceException;
import dev.sahilbasumatary.replayservice.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Reads match metadata and the point-by-point ledger from match-service. */
@Component
public class MatchDataClient {

    private final RestClient restClient;

    public MatchDataClient(@Qualifier("matchServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public MatchSummary fetchMatch(UUID matchId) {
        try {
            return restClient
                    .get()
                    .uri("/api/matches/{matchId}", matchId)
                    .retrieve()
                    .body(MatchSummary.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Match", matchId);
            }
            throw new DownstreamServiceException("Failed to load match " + matchId, ex);
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("match-service is unavailable", ex);
        }
    }

    public List<MatchPointSummary> fetchPoints(UUID matchId) {
        try {
            return restClient
                    .get()
                    .uri("/api/matches/{matchId}/points", matchId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<MatchPointSummary>>() {});
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Match", matchId);
            }
            throw new DownstreamServiceException("Failed to load points for match " + matchId, ex);
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("match-service is unavailable", ex);
        }
    }
}
