package dev.sahilbasumatary.analyticsservice.client;

import dev.sahilbasumatary.analyticsservice.client.dto.CompletedMatchFeedResponse;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchPointSummary;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchSummary;
import dev.sahilbasumatary.analyticsservice.exception.DownstreamServiceException;
import dev.sahilbasumatary.analyticsservice.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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

    public CompletedMatchFeedResponse fetchCompletedMatchIds(UUID cursor, int limit) {
        try {
            return restClient
                    .get()
                    .uri(
                            uriBuilder ->
                                    uriBuilder
                                            .path("/internal/matches/completed")
                                            .queryParam("limit", limit)
                                            .queryParamIfPresent("cursor", java.util.Optional.ofNullable(cursor))
                                            .build())
                    .retrieve()
                    .body(CompletedMatchFeedResponse.class);
        } catch (RestClientResponseException ex) {
            throw new DownstreamServiceException("Failed to load completed match feed", ex);
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("match-service is unavailable", ex);
        }
    }
}
