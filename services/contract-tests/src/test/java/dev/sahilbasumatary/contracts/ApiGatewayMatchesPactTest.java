package dev.sahilbasumatary.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "match-service")
class ApiGatewayMatchesPactTest {

    private static final String MATCH_ID = "22222222-2222-2222-2222-222222222222";

    @Pact(consumer = "api-gateway")
    V4Pact listMatchesWhenMatchesExist(PactBuilder builder) {
        return builder.usingLegacyDsl()
                .given("matches exist")
                .uponReceiving("GET /api/matches")
                .path("/api/matches")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(matchListExample())
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "api-gateway")
    V4Pact getMatchWhenMatchExists(PactBuilder builder) {
        return builder.usingLegacyDsl()
                .given("match exists")
                .uponReceiving("GET /api/matches/{id}")
                .path("/api/matches/" + MATCH_ID)
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(matchObjectExample())
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "listMatchesWhenMatchesExist")
    void fetchesMatchesFromProviderContract(MockServer mockServer)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(mockServer.getUrl() + "/api/matches"))
                        .GET()
                        .timeout(Duration.ofSeconds(5))
                        .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("atp-finals-m1").contains("Novak Djokovic");
    }

    @Test
    @PactTestFor(pactMethod = "getMatchWhenMatchExists")
    void fetchesMatchByIdFromProviderContract(MockServer mockServer)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(mockServer.getUrl() + "/api/matches/" + MATCH_ID))
                        .GET()
                        .timeout(Duration.ofSeconds(5))
                        .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(MATCH_ID).contains("atp-finals-m1");
    }

    private static DslPart matchListExample() {
        PactDslJsonBody match = PactDslJsonArray.arrayMinLike(1);
        fillMatchFields(match);
        return match.closeObject();
    }

    private static PactDslJsonBody matchObjectExample() {
        PactDslJsonBody match = new PactDslJsonBody();
        fillMatchFields(match);
        return match;
    }

    private static void fillMatchFields(PactDslJsonBody match) {
        match.uuid("id", MATCH_ID)
                .stringType("externalId", "atp-finals-m1")
                .uuid("tournamentId", "33333333-3333-3333-3333-333333333333")
                .stringMatcher("surface", "HARD|CLAY|GRASS", "HARD")
                .stringMatcher(
                        "status",
                        "SCHEDULED|IN_PROGRESS|SUSPENDED|COMPLETED|CANCELLED",
                        "SCHEDULED")
                .integerType("bestOfSets", 3)
                .stringType("scheduledAt", "2024-06-01T12:00:00Z")
                .stringType("startedAt", "2024-06-01T12:05:00Z")
                .stringType("endedAt", "2024-06-01T14:00:00Z");
        match.object("metadata").closeObject();
        match.object("currentScore").closeObject();
        match.eachLike("players")
                .uuid("id", "44444444-4444-4444-4444-444444444444")
                .uuid("playerId", "11111111-1111-1111-1111-111111111111")
                .stringType("displayName", "Novak Djokovic")
                .stringMatcher("side", "HOME|AWAY", "HOME")
                .integerType("seedNumber", 1)
                .closeArray();
        match.integerType("pointsPlayed", 0)
                .stringType("createdAt", "2024-01-01T12:00:00Z")
                .stringType("updatedAt", "2024-01-01T12:00:00Z");
    }
}
