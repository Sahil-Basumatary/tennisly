package dev.sahilbasumatary.tennisdataservice.livetennis;

import dev.sahilbasumatary.tennisdataservice.dto.UpstreamMatchData;
import dev.sahilbasumatary.tennisdataservice.dto.UpstreamPointData;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
public class LiveTennisClient {

    private static final Logger log = LoggerFactory.getLogger(LiveTennisClient.class);

    private final RestTemplate restTemplate;
    private final Clock clock;
    private final String baseUrl;
    private final String apiKey;
    private final int maxAttempts;
    private final TokenBucketRateLimiter rateLimiter;
    private final SimpleCircuitBreaker circuitBreaker;

    public LiveTennisClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${tennis.data.livetennis.base-url}") String baseUrl,
            @Value("${tennis.data.livetennis.api-key:}") String apiKey,
            @Value("${tennis.data.livetennis.max-attempts:3}") int maxAttempts,
            @Value("${tennis.data.livetennis.requests-per-minute:60}") int requestsPerMinute,
            @Value("${tennis.data.livetennis.circuit-breaker.failure-threshold:5}")
                    int failureThreshold,
            @Value("${tennis.data.livetennis.circuit-breaker.open-duration:PT2M}")
                    Duration openDuration) {
        this.restTemplate =
                restTemplateBuilder
                        .setConnectTimeout(Duration.ofSeconds(5))
                        .setReadTimeout(Duration.ofSeconds(20))
                        .build();
        this.clock = Clock.systemUTC();
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.maxAttempts = maxAttempts;
        this.rateLimiter = new TokenBucketRateLimiter(requestsPerMinute, clock);
        this.circuitBreaker = new SimpleCircuitBreaker(failureThreshold, openDuration, clock);
    }

    @PostConstruct
    void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Live Tennis API requires TENNIS_LIVETENNIS_API_KEY");
        }
    }

    public List<UpstreamMatchData> listMatches(String status, String tour, int limit, int offset) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl(baseUrl + "/matches")
                        .queryParam("status", status)
                        .queryParam("limit", limit)
                        .queryParam("offset", offset);
        if (tour != null && !tour.isBlank()) {
            builder.queryParam("tour", tour);
        }
        LiveTennisModels.PageResponse page =
                request(builder.build().toUriString(), LiveTennisModels.PageResponse.class);
        if (page == null || page.data() == null) {
            return List.of();
        }
        List<UpstreamMatchData> matches = new ArrayList<>(page.data().size());
        for (LiveTennisModels.MatchPayload payload : page.data()) {
            matches.add(mapMatch(payload));
        }
        return matches;
    }

    public List<UpstreamMatchData> listHistoryMatches(int limit, int offset) {
        String url =
                UriComponentsBuilder.fromHttpUrl(baseUrl + "/history/matches")
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .build()
                        .toUriString();
        LiveTennisModels.PageResponse page = request(url, LiveTennisModels.PageResponse.class);
        if (page == null || page.data() == null) {
            return List.of();
        }
        return page.data().stream().map(this::mapMatch).toList();
    }

    public UpstreamMatchData getMatch(long matchId) {
        LiveTennisModels.MatchPayload payload =
                request(baseUrl + "/matches/" + matchId, LiveTennisModels.MatchPayload.class);
        return mapMatch(payload);
    }

    public List<UpstreamPointData> getPointTape(long matchId) {
        LiveTennisModels.HistoryTapeResponse tape =
                request(
                        baseUrl + "/history/matches/" + matchId,
                        LiveTennisModels.HistoryTapeResponse.class);
        if (tape == null) {
            return List.of();
        }
        return PointTapeDecoder.decode(tape.tape());
    }

    private UpstreamMatchData mapMatch(LiveTennisModels.MatchPayload payload) {
        if (payload == null) {
            throw new IllegalStateException("Live Tennis match payload was empty");
        }
        return new UpstreamMatchData(
                payload.id(),
                "lta-" + payload.id(),
                payload.tournament(),
                normalizeSurface(payload.surface()),
                payload.format(),
                payload.round(),
                payload.status(),
                Boolean.TRUE.equals(payload.doubles()),
                Boolean.TRUE.equals(payload.indoor()),
                parseInstant(payload.scheduledTime()),
                mapPlayer(payload.players() == null ? null : payload.players().p1()),
                mapPlayer(payload.players() == null ? null : payload.players().p2()),
                payload.winner(),
                mapScore(payload.score()));
    }

    private UpstreamMatchData.UpstreamPlayerRef mapPlayer(LiveTennisModels.PlayerPayload player) {
        if (player == null) {
            return new UpstreamMatchData.UpstreamPlayerRef(null, "", "", "");
        }
        String display = firstNonBlank(player.name(), joinName(player.firstName(), player.lastName()));
        String[] parts = splitDisplayName(display);
        return new UpstreamMatchData.UpstreamPlayerRef(
                player.id(),
                firstNonBlank(player.firstName(), parts[0]),
                firstNonBlank(player.lastName(), parts[1]),
                display);
    }

    private UpstreamMatchData.UpstreamScoreSnapshot mapScore(LiveTennisModels.ScorePayload score) {
        if (score == null) {
            return null;
        }
        return new UpstreamMatchData.UpstreamScoreSnapshot(
                score.sets() == null ? List.of() : score.sets(),
                score.games() == null ? List.of() : score.games(),
                score.points() == null ? List.of() : score.points(),
                score.server(),
                Boolean.TRUE.equals(score.tiebreak()));
    }

    private <T> T request(String url, Class<T> type) {
        return circuitBreaker.execute(
                () -> {
                    rateLimiter.acquire();
                    Exception lastFailure = null;
                    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                        try {
                            ResponseEntity<T> response =
                                    restTemplate.exchange(
                                            url,
                                            HttpMethod.GET,
                                            new HttpEntity<>(headers()),
                                            type);
                            circuitBreaker.recordSuccess();
                            return response.getBody();
                        } catch (HttpStatusCodeException ex) {
                            lastFailure = ex;
                            if (!shouldRetry(ex) || attempt == maxAttempts) {
                                circuitBreaker.recordFailure();
                                throw ex;
                            }
                            sleep(backoff(attempt));
                        } catch (RestClientException ex) {
                            lastFailure = ex;
                            if (attempt == maxAttempts) {
                                circuitBreaker.recordFailure();
                                throw ex;
                            }
                            sleep(backoff(attempt));
                        }
                    }
                    circuitBreaker.recordFailure();
                    throw lastFailure instanceof RuntimeException runtime
                            ? runtime
                            : new IllegalStateException("Live Tennis request failed", lastFailure);
                });
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        return headers;
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
            throw new IllegalStateException("Interrupted while waiting to retry Live Tennis", ex);
        }
    }

    private static String normalizeSurface(String raw) {
        if (raw == null || raw.isBlank()) {
            return "HARD";
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "clay" -> "CLAY";
            case "grass" -> "GRASS";
            default -> "HARD";
        };
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String[] splitDisplayName(String display) {
        if (display == null || display.isBlank()) {
            return new String[] {"", ""};
        }
        String trimmed = display.trim();
        int space = trimmed.lastIndexOf(' ');
        if (space < 0) {
            return new String[] {"", trimmed};
        }
        return new String[] {trimmed.substring(0, space).trim(), trimmed.substring(space + 1).trim()};
    }

    private static String joinName(String first, String last) {
        String f = first == null ? "" : first.trim();
        String l = last == null ? "" : last.trim();
        return (f + " " + l).trim();
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first.trim() : (fallback == null ? "" : fallback);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /** Same resilience helpers as BallDontLie — kept local so livetennis stays self-contained. */
    static final class TokenBucketRateLimiter {
        private final int requestsPerMinute;
        private final Clock clock;
        private final long[] stamps;
        private int index;

        TokenBucketRateLimiter(int requestsPerMinute, Clock clock) {
            this.requestsPerMinute = Math.max(1, requestsPerMinute);
            this.clock = clock;
            this.stamps = new long[this.requestsPerMinute];
        }

        synchronized void acquire() {
            while (true) {
                long now = clock.millis();
                long oldest = stamps[index];
                if (oldest == 0L || now - oldest >= 60_000L) {
                    stamps[index] = now;
                    index = (index + 1) % stamps.length;
                    return;
                }
                long wait = 60_000L - (now - oldest);
                try {
                    Thread.sleep(Math.max(1L, wait));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Interrupted while waiting for Live Tennis rate limit", ex);
                }
            }
        }
    }

    static final class SimpleCircuitBreaker {
        private final int failureThreshold;
        private final Duration openDuration;
        private final Clock clock;
        private int failures;
        private Instant openUntil;

        SimpleCircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
            this.failureThreshold = Math.max(1, failureThreshold);
            this.openDuration = openDuration == null ? Duration.ofMinutes(2) : openDuration;
            this.clock = clock;
        }

        <T> T execute(Supplier<T> supplier) {
            Instant now = Instant.now(clock);
            if (openUntil != null && now.isBefore(openUntil)) {
                throw new IllegalStateException(
                        "Live Tennis circuit breaker is open until " + openUntil);
            }
            return supplier.get();
        }

        void recordSuccess() {
            failures = 0;
            openUntil = null;
        }

        void recordFailure() {
            failures += 1;
            if (failures >= failureThreshold) {
                openUntil = Instant.now(clock).plus(openDuration);
                log.warn("Live Tennis circuit opened until {}", openUntil);
            }
        }
    }
}
