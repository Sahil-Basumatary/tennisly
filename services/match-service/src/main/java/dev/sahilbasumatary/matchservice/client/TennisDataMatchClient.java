package dev.sahilbasumatary.matchservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
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
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TennisDataMatchClient {

    private static final Logger log = LoggerFactory.getLogger(TennisDataMatchClient.class);

    private final RestClient restClient;

    public TennisDataMatchClient(@Qualifier("tennisDataServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<UpstreamMatchDto> listMatches(String status, int limit, int offset) {
        try {
            String uri =
                    UriComponentsBuilder.fromPath("/api/tennis/matches")
                            .queryParam("status", status)
                            .queryParam("limit", limit)
                            .queryParam("offset", offset)
                            .toUriString();
            UpstreamMatchDto[] body =
                    restClient.get().uri(uri).retrieve().body(UpstreamMatchDto[].class);
            if (body == null) {
                return List.of();
            }
            return List.of(body);
        } catch (RestClientException ex) {
            log.warn("Failed to list upstream matches status={}: {}", status, ex.getMessage());
            return List.of();
        }
    }

    public List<UpstreamPointDto> listPoints(long ltaId) {
        try {
            UpstreamPointDto[] body =
                    restClient
                            .get()
                            .uri("/api/tennis/matches/{ltaId}/points", ltaId)
                            .retrieve()
                            .body(UpstreamPointDto[].class);
            if (body == null) {
                return List.of();
            }
            return List.of(body);
        } catch (RestClientException ex) {
            log.warn("Failed to load point tape for ltaId={}: {}", ltaId, ex.getMessage());
            return List.of();
        }
    }

    public Optional<ResolvedPlayerDto> resolvePlayer(
            Long providerPlayerId,
            String firstName,
            String lastName,
            String displayName,
            String gender) {
        try {
            String uri =
                    UriComponentsBuilder.fromPath("/api/tennis/players/resolve")
                            .queryParam("providerPlayerId", providerPlayerId)
                            .queryParam("firstName", firstName)
                            .queryParam("lastName", lastName)
                            .queryParam("displayName", displayName)
                            .queryParam("gender", gender)
                            .toUriString();
            ResolvedPlayerDto body =
                    restClient.post().uri(uri).retrieve().body(ResolvedPlayerDto.class);
            return Optional.ofNullable(body);
        } catch (RestClientResponseException ex) {
            log.warn("Player resolve failed for {}: {}", displayName, ex.getMessage());
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn("Player resolve unreachable for {}: {}", displayName, ex.getMessage());
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpstreamMatchDto(
            long providerMatchId,
            String externalId,
            String tournamentName,
            String surface,
            String format,
            String round,
            String status,
            boolean doubles,
            boolean indoor,
            Instant scheduledAt,
            PlayerSide home,
            PlayerSide away,
            Integer winnerSide) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerSide(
            Long providerPlayerId, String firstName, String lastName, String displayName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpstreamPointDto(
            int sequenceNumber,
            int serverSide,
            int winnerSide,
            String outcome,
            Integer rallyLength,
            Map<String, Object> scoreSnapshot) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResolvedPlayerDto(
            UUID id,
            String externalId,
            String firstName,
            String lastName,
            String nationality,
            String gender) {}
}
