package dev.sahilbasumatary.tennisdataservice.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.sahilbasumatary.tennisdataservice.dto.PlayerData;
import dev.sahilbasumatary.tennisdataservice.dto.RankingData;
import dev.sahilbasumatary.tennisdataservice.dto.TournamentData;
import dev.sahilbasumatary.tennisdataservice.entity.Backhand;
import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Hand;
import dev.sahilbasumatary.tennisdataservice.entity.RankingType;
import dev.sahilbasumatary.tennisdataservice.entity.Surface;
import dev.sahilbasumatary.tennisdataservice.entity.TournamentLevel;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(name = "tennis.data.provider", havingValue = "balldontlie")
public class BallDontLieProvider implements TennisDataProvider {

    private static final Logger log = LoggerFactory.getLogger(BallDontLieProvider.class);

    private final RestTemplate restTemplate;
    private final Clock clock;
    private final String baseUrl;
    private final String apiKey;
    private final int maxPages;
    private final int maxAttempts;
    private final int perPage;
    private final TokenBucketRateLimiter rateLimiter;
    private final SimpleCircuitBreaker circuitBreaker;

    public BallDontLieProvider(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${tennis.data.balldontlie.base-url:https://api.balldontlie.io}")
                    String baseUrl,
            @Value("${tennis.data.balldontlie.api-key:}") String apiKey,
            @Value("${tennis.data.balldontlie.max-pages:5}") int maxPages,
            @Value("${tennis.data.balldontlie.max-attempts:3}") int maxAttempts,
            @Value("${tennis.data.balldontlie.per-page:100}") int perPage,
            @Value("${tennis.data.balldontlie.requests-per-minute:5}") int requestsPerMinute,
            @Value("${tennis.data.balldontlie.circuit-breaker.failure-threshold:5}")
                    int failureThreshold,
            @Value("${tennis.data.balldontlie.circuit-breaker.open-duration:PT2M}")
                    Duration openDuration) {
        this.restTemplate =
                restTemplateBuilder
                        .setConnectTimeout(Duration.ofSeconds(5))
                        .setReadTimeout(Duration.ofSeconds(15))
                        .build();
        this.clock = Clock.systemUTC();
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.maxPages = maxPages;
        this.maxAttempts = maxAttempts;
        this.perPage = Math.max(1, perPage);
        this.rateLimiter = new TokenBucketRateLimiter(requestsPerMinute, clock);
        this.circuitBreaker = new SimpleCircuitBreaker(failureThreshold, openDuration, clock);
    }

    @PostConstruct
    void validateApiKey() {
        requireApiKey();
    }

    @Override
    public List<PlayerData> fetchPlayers() {
        List<PlayerData> players = new ArrayList<>();
        players.addAll(fetchPlayerPage("/atp/v1/players", Gender.MALE));
        players.addAll(fetchPlayerPage("/wta/v1/players", Gender.FEMALE));
        return players;
    }

    @Override
    public Optional<PlayerData> fetchPlayerById(String externalId) {
        TourAndId tourAndId = TourAndId.fromExternalId(externalId);
        if (tourAndId == null) {
            return Optional.empty();
        }
        PlayerApiResponse response =
                request(
                        UriComponentsBuilder.fromHttpUrl(baseUrl + tourAndId.playerPath())
                                .build()
                                .toUriString(),
                        PlayerApiResponse.class);
        return Optional.ofNullable(response.data()).map(player -> mapPlayer(player, tourAndId.gender()));
    }

    @Override
    public List<RankingData> fetchRankings() {
        List<RankingData> rankings = new ArrayList<>();
        rankings.addAll(fetchRankingPage("/atp/v1/rankings", Gender.MALE));
        rankings.addAll(fetchRankingPage("/wta/v1/rankings", Gender.FEMALE));
        return rankings;
    }

    @Override
    public List<TournamentData> fetchTournaments() {
        List<TournamentData> tournaments = new ArrayList<>();
        tournaments.addAll(fetchTournamentPage("/atp/v1/tournaments", Gender.MALE));
        tournaments.addAll(fetchTournamentPage("/wta/v1/tournaments", Gender.FEMALE));
        return tournaments;
    }

    private List<PlayerData> fetchPlayerPage(String path, Gender gender) {
        return fetchPages(path, PlayerListResponse.class).stream()
                .flatMap(response -> response.data().stream())
                .map(player -> mapPlayer(player, gender))
                .toList();
    }

    private List<RankingData> fetchRankingPage(String path, Gender gender) {
        return fetchPages(path, RankingListResponse.class).stream()
                .flatMap(response -> response.data().stream())
                .map(ranking -> mapRanking(ranking, gender))
                .flatMap(Optional::stream)
                .toList();
    }

    private List<TournamentData> fetchTournamentPage(String path, Gender gender) {
        return fetchPages(path, TournamentListResponse.class).stream()
                .flatMap(response -> response.data().stream())
                .map(tournament -> mapTournament(tournament, gender))
                .toList();
    }

    private <T extends PageResponse> List<T> fetchPages(String path, Class<T> responseType) {
        List<T> responses = new ArrayList<>();
        Integer cursor = null;
        for (int page = 0; page < maxPages; page++) {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.fromHttpUrl(baseUrl + path)
                            .queryParam("per_page", perPage);
            if (cursor != null) {
                builder.queryParam("cursor", cursor);
            }
            T response = request(builder.build().toUriString(), responseType);
            responses.add(response);
            cursor = response.meta() == null ? null : response.meta().nextCursor();
            if (cursor == null) {
                break;
            }
        }
        return responses;
    }

    private <T> T request(String url, Class<T> responseType) {
        requireApiKey();
        return circuitBreaker.execute(
                () -> {
                    RuntimeException lastFailure = null;
                    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                        rateLimiter.acquire();
                        try {
                            ResponseEntity<T> response =
                                    restTemplate.exchange(
                                            url,
                                            HttpMethod.GET,
                                            new HttpEntity<>(headers()),
                                            responseType);
                            return response.getBody();
                        } catch (HttpStatusCodeException ex) {
                            if (!shouldRetry(ex) || attempt == maxAttempts) {
                                throw ex;
                            }
                            lastFailure = ex;
                            sleep(backoff(attempt));
                        } catch (RestClientException ex) {
                            if (attempt == maxAttempts) {
                                throw ex;
                            }
                            lastFailure = ex;
                            sleep(backoff(attempt));
                        }
                    }
                    throw lastFailure == null
                            ? new IllegalStateException("BALLDONTLIE request failed")
                            : lastFailure;
                });
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "BALLDONTLIE provider requires TENNIS_BALLDONTLIE_API_KEY");
        }
    }

    private boolean shouldRetry(HttpStatusCodeException ex) {
        return ex.getStatusCode().value() == 429 || ex.getStatusCode().is5xxServerError();
    }

    private Duration backoff(int attempt) {
        return Duration.ofSeconds(Math.min(30, attempt * attempt));
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry BALLDONTLIE", ex);
        }
    }

    private PlayerData mapPlayer(ApiPlayer player, Gender gender) {
        return new PlayerData(
                externalId(gender, player.id()),
                firstName(player),
                lastName(player),
                firstNonBlank(player.countryCode(), player.country()),
                null,
                mapHand(player.plays()),
                Backhand.TWO_HANDED,
                player.heightCm(),
                player.weightKg(),
                player.turnedPro(),
                null,
                null,
                gender);
    }

    private Optional<RankingData> mapRanking(ApiRanking ranking, Gender gender) {
        if (ranking.player() == null || ranking.player().id() == null) {
            log.warn("Skipping ranking without player id rankingId={}", ranking.id());
            return Optional.empty();
        }
        return Optional.of(
                new RankingData(
                        externalId(gender, ranking.player().id()),
                        ranking.rank(),
                        ranking.points(),
                        ranking.rankingDate() == null ? LocalDate.now(clock) : ranking.rankingDate(),
                        RankingType.SINGLES,
                        gender));
    }

    private TournamentData mapTournament(ApiTournament tournament, Gender gender) {
        Location location = Location.from(tournament.location());
        return new TournamentData(
                externalId(gender, tournament.id()),
                firstNonBlank(tournament.name(), "Unknown tournament " + tournament.id()),
                mapTournamentLevel(tournament.category(), gender),
                mapSurface(tournament.surface()),
                gender,
                location.city(),
                location.country(),
                null);
    }

    private String firstName(ApiPlayer player) {
        if (hasText(player.firstName())) {
            return player.firstName();
        }
        String[] names = splitName(player.fullName());
        return names[0];
    }

    private String lastName(ApiPlayer player) {
        if (hasText(player.lastName())) {
            return player.lastName();
        }
        String[] names = splitName(player.fullName());
        return names[1];
    }

    private String[] splitName(String fullName) {
        if (!hasText(fullName)) {
            return new String[] {"Unknown", "Player"};
        }
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length == 1 ? new String[] {parts[0], "Player"} : parts;
    }

    private Hand mapHand(String plays) {
        if (!hasText(plays)) {
            return Hand.RIGHT;
        }
        return plays.toLowerCase(Locale.ROOT).contains("left") ? Hand.LEFT : Hand.RIGHT;
    }

    private Surface mapSurface(String surface) {
        if (!hasText(surface)) {
            return Surface.HARD;
        }
        return switch (surface.trim().toUpperCase(Locale.ROOT)) {
            case "CLAY" -> Surface.CLAY;
            case "GRASS" -> Surface.GRASS;
            default -> Surface.HARD;
        };
    }

    private TournamentLevel mapTournamentLevel(String category, Gender gender) {
        if (!hasText(category)) {
            return TournamentLevel.OTHER;
        }
        String normalized = category.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        if ("GRAND_SLAM".equals(normalized)) {
            return TournamentLevel.GRAND_SLAM;
        }
        if (gender == Gender.MALE) {
            return switch (normalized) {
                case "ATP_1000" -> TournamentLevel.ATP_1000;
                case "ATP_500" -> TournamentLevel.ATP_500;
                case "ATP_250" -> TournamentLevel.ATP_250;
                default -> TournamentLevel.OTHER;
            };
        }
        return switch (normalized) {
            case "WTA_1000" -> TournamentLevel.WTA_1000;
            case "WTA_500" -> TournamentLevel.WTA_500;
            case "WTA_250" -> TournamentLevel.WTA_250;
            default -> TournamentLevel.OTHER;
        };
    }

    private static String externalId(Gender gender, Integer id) {
        String prefix = gender == Gender.MALE ? "atp" : "wta";
        return prefix + "-" + id;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstNonBlank(String first, String fallback) {
        return hasText(first) ? first : fallback;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private interface PageResponse {
        Pagination meta();
    }

    private record PlayerListResponse(List<ApiPlayer> data, Pagination meta) implements PageResponse {}

    private record PlayerApiResponse(ApiPlayer data) {}

    private record RankingListResponse(List<ApiRanking> data, Pagination meta) implements PageResponse {}

    private record TournamentListResponse(List<ApiTournament> data, Pagination meta)
            implements PageResponse {}

    private record Pagination(@JsonProperty("next_cursor") Integer nextCursor) {}

    private record ApiPlayer(
            Integer id,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            @JsonProperty("full_name") String fullName,
            String country,
            @JsonProperty("country_code") String countryCode,
            @JsonProperty("height_cm") Integer heightCm,
            @JsonProperty("weight_kg") Integer weightKg,
            String plays,
            @JsonProperty("turned_pro") Integer turnedPro) {}

    private record ApiRanking(
            Integer id,
            ApiPlayer player,
            Integer rank,
            Integer points,
            @JsonProperty("ranking_date") LocalDate rankingDate) {}

    private record ApiTournament(
            Integer id, String name, String location, String surface, String category) {}

    private record Location(String city, String country) {

        static Location from(String raw) {
            if (!hasText(raw)) {
                return new Location(null, null);
            }
            String[] parts = raw.split(",", 2);
            if (parts.length == 1) {
                return new Location(parts[0].trim(), null);
            }
            return new Location(parts[0].trim(), parts[1].trim());
        }
    }

    private record TourAndId(Gender gender, String id) {

        static TourAndId fromExternalId(String externalId) {
            if (!hasText(externalId) || !externalId.contains("-")) {
                return null;
            }
            String[] parts = externalId.split("-", 2);
            if ("atp".equals(parts[0])) {
                return new TourAndId(Gender.MALE, parts[1]);
            }
            if ("wta".equals(parts[0])) {
                return new TourAndId(Gender.FEMALE, parts[1]);
            }
            return null;
        }

        String playerPath() {
            String tour = gender == Gender.MALE ? "atp" : "wta";
            return "/" + tour + "/v1/players/" + id;
        }
    }

    private static final class TokenBucketRateLimiter {

        private final int maxRequestsPerMinute;
        private final Clock clock;
        private int remaining;
        private Instant windowStart;

        private TokenBucketRateLimiter(int maxRequestsPerMinute, Clock clock) {
            this.maxRequestsPerMinute = Math.max(1, maxRequestsPerMinute);
            this.clock = clock;
            this.remaining = this.maxRequestsPerMinute;
            this.windowStart = Instant.now(clock);
        }

        void acquire() {
            Duration wait = reserveOrWait();
            if (wait.isZero() || wait.isNegative()) {
                return;
            }
            try {
                Thread.sleep(Math.max(1L, wait.toMillis()));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for BALLDONTLIE rate limit", ex);
            }
            acquire();
        }

        private synchronized Duration reserveOrWait() {
            Instant now = Instant.now(clock);
            Duration elapsed = Duration.between(windowStart, now);
            if (elapsed.compareTo(Duration.ofMinutes(1)) >= 0) {
                windowStart = now;
                remaining = maxRequestsPerMinute;
            }
            if (remaining > 0) {
                remaining--;
                return Duration.ZERO;
            }
            return Duration.ofMinutes(1).minus(elapsed);
        }
    }

    private static final class SimpleCircuitBreaker {

        private final int failureThreshold;
        private final Duration openDuration;
        private final Clock clock;
        private int consecutiveFailures;
        private Instant openUntil;

        private SimpleCircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
            this.failureThreshold = Math.max(1, failureThreshold);
            this.openDuration = openDuration;
            this.clock = clock;
        }

        synchronized <T> T execute(Supplier<T> supplier) {
            Instant now = Instant.now(clock);
            if (openUntil != null && now.isBefore(openUntil)) {
                throw new IllegalStateException("BALLDONTLIE circuit breaker is open until " + openUntil);
            }
            try {
                T result = supplier.get();
                consecutiveFailures = 0;
                openUntil = null;
                return result;
            } catch (RuntimeException ex) {
                consecutiveFailures++;
                if (consecutiveFailures >= failureThreshold) {
                    openUntil = Instant.now(clock).plus(openDuration);
                }
                throw ex;
            }
        }
    }
}
