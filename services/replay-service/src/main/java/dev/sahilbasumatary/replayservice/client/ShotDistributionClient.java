package dev.sahilbasumatary.replayservice.client;

import dev.sahilbasumatary.replayservice.client.dto.ShotDistributionModel;
import dev.sahilbasumatary.replayservice.domain.Surface;
import dev.sahilbasumatary.replayservice.exception.DownstreamServiceException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Reads shot distribution models (the synthetic priors) from tennis-data-service. */
@Component
public class ShotDistributionClient {

    private final RestClient restClient;

    public ShotDistributionClient(@Qualifier("tennisDataServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ShotDistributionModel> fetchBySurface(Surface surface) {
        try {
            return restClient
                    .get()
                    .uri(
                            uriBuilder ->
                                    uriBuilder
                                            .path("/api/tennis/shot-distributions")
                                            .queryParam("surface", surface.name())
                                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ShotDistributionModel>>() {});
        } catch (RestClientException ex) {
            throw new DownstreamServiceException(
                    "Failed to load shot distributions for surface " + surface, ex);
        }
    }
}
