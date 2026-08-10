package dev.sahilbasumatary.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MockServer;
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
@PactTestFor(providerName = "user-service")
class ApiGatewayWebhooksPactTest {

    private static final String ORG_ID = "55555555-5555-5555-5555-555555555555";

    @Pact(consumer = "api-gateway")
    V4Pact listWebhooksWhenEndpointsExist(PactBuilder builder) {
        PactDslJsonBody item = PactDslJsonArray.arrayMinLike(1);
        item.uuid("id", "66666666-6666-6666-6666-666666666666")
                .uuid("organizationId", ORG_ID)
                .stringType("name", "match-hooks")
                .stringType("targetUrl", "https://hooks.example.com/tennisly")
                .stringType("secretPrefix", "whsec_abc12345");
        item.array("eventTypes").stringType("match.completed").closeArray();
        item.booleanType("active", true)
                .stringType("description", "prod deliveries")
                .stringType("createdByClerkId", "user_abc")
                .stringType("revokedAt", "2024-01-02T00:00:00Z")
                .stringType("lastDeliveryAt", "2024-01-03T00:00:00Z")
                .stringType("createdAt", "2024-01-01T12:00:00Z")
                .stringType("updatedAt", "2024-01-01T12:00:00Z");
        return builder.usingLegacyDsl()
                .given("webhook endpoints exist for org")
                .uponReceiving("GET /api/users/public/webhooks")
                .path("/api/users/public/webhooks")
                .method("GET")
                .headers(Map.of("X-Org-Id", ORG_ID))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(item.closeObject())
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "listWebhooksWhenEndpointsExist")
    void fetchesWebhooksFromProviderContract(MockServer mockServer)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(mockServer.getUrl() + "/api/users/public/webhooks"))
                        .header("X-Org-Id", ORG_ID)
                        .GET()
                        .timeout(Duration.ofSeconds(5))
                        .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("match-hooks").contains(ORG_ID);
    }
}
