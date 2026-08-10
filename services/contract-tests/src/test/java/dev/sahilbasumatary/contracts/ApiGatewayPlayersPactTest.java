package dev.sahilbasumatary.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
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
@PactTestFor(providerName = "tennis-data-service")
class ApiGatewayPlayersPactTest {

    @Pact(consumer = "api-gateway")
    V4Pact listPlayersWhenPlayersExist(PactBuilder builder) {
        return builder.usingLegacyDsl()
                .given("players exist")
                .uponReceiving("GET /api/tennis/players")
                .path("/api/tennis/players")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(
                        PactDslJsonArray.arrayMinLike(1)
                                .uuid("id", "11111111-1111-1111-1111-111111111111")
                                .stringType("externalId", "atp-100")
                                .stringType("firstName", "Novak")
                                .stringType("lastName", "Djokovic")
                                .stringType("nationality", "SRB")
                                .stringMatcher(
                                        "dateOfBirth",
                                        "\\d{4}-\\d{2}-\\d{2}",
                                        "1987-05-22")
                                .stringMatcher("hand", "LEFT|RIGHT", "RIGHT")
                                .stringMatcher(
                                        "backhand", "ONE_HANDED|TWO_HANDED", "TWO_HANDED")
                                .integerType("heightCm", 188)
                                .integerType("weightKg", 77)
                                .integerType("proYear", 2003)
                                .integerType("currentRanking", 1)
                                .integerType("currentPoints", 11000)
                                .stringMatcher("gender", "MALE|FEMALE", "MALE")
                                .stringType("createdAt", "2024-01-01T12:00:00Z")
                                .stringType("updatedAt", "2024-01-01T12:00:00Z")
                                .closeObject())
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "listPlayersWhenPlayersExist")
    void fetchesPlayersFromProviderContract(MockServer mockServer)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(mockServer.getUrl() + "/api/tennis/players"))
                        .GET()
                        .timeout(Duration.ofSeconds(5))
                        .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Novak").contains("Djokovic");
    }
}
