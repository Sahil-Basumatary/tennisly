package dev.sahilbasumatary.apigateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.sahilbasumatary.apigateway.client.ApiKeyValidationResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

class ApiKeyAuthHeadersTest {

    private static final UUID ORG_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID KEY_ID = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

    @Test
    void applyTrustedHeadersStripsSpoofedValuesAndSetsTrustedOnes() {
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/v1/players")
                        .header(JwtClaimsForwardingFilter.X_USER_ID, "attacker")
                        .header(JwtClaimsForwardingFilter.X_ORG_ID, "attacker-org")
                        .header(JwtClaimsForwardingFilter.X_USER_ROLES, "ADMIN")
                        .header(ApiKeyAuthHeaders.X_API_KEY_ID, "fake-key")
                        .header(ApiKeyAuthHeaders.X_API_KEY_SCOPES, "admin")
                        .header(ApiKeyAuthHeaders.X_API_KEY, "tly_live_secret")
                        .build();
        var validation =
                new ApiKeyValidationResponse(
                        ORG_ID, KEY_ID, List.of("read", "players"), "PRO", "Baseline Club");
        var mutated = ApiKeyAuthHeaders.applyTrustedHeaders(request, validation);
        assertNull(mutated.getHeaders().getFirst(JwtClaimsForwardingFilter.X_USER_ID));
        assertNull(mutated.getHeaders().getFirst(JwtClaimsForwardingFilter.X_USER_ROLES));
        assertNull(mutated.getHeaders().getFirst(ApiKeyAuthHeaders.X_API_KEY));
        assertEquals(ORG_ID.toString(), mutated.getHeaders().getFirst(ApiKeyAuthHeaders.X_ORG_ID));
        assertEquals(KEY_ID.toString(), mutated.getHeaders().getFirst(ApiKeyAuthHeaders.X_API_KEY_ID));
        assertEquals(
                "read,players", mutated.getHeaders().getFirst(ApiKeyAuthHeaders.X_API_KEY_SCOPES));
        assertEquals("PRO", mutated.getHeaders().getFirst(ApiKeyAuthHeaders.X_PLAN_TIER));
    }

    @Test
    void applyTrustedHeadersDefaultsNullPlanTierToFree() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/players").build();
        var validation =
                new ApiKeyValidationResponse(
                        ORG_ID, KEY_ID, List.of("read"), null, "Baseline Club");
        var mutated = ApiKeyAuthHeaders.applyTrustedHeaders(request, validation);
        assertEquals("FREE", mutated.getHeaders().getFirst(ApiKeyAuthHeaders.X_PLAN_TIER));
    }

    @Test
    void applyTrustedHeadersStripsSpoofedPlanTier() {
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/v1/players")
                        .header(ApiKeyAuthHeaders.X_PLAN_TIER, "ENTERPRISE")
                        .build();
        var validation =
                new ApiKeyValidationResponse(
                        ORG_ID, KEY_ID, List.of("read"), "BASIC", "Baseline Club");
        var mutated = ApiKeyAuthHeaders.applyTrustedHeaders(request, validation);
        assertEquals("BASIC", mutated.getHeaders().getFirst(ApiKeyAuthHeaders.X_PLAN_TIER));
    }

    @Test
    void stripSpoofableHeadersRemovesIdentityHeadersOnly() {
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/v1/players")
                        .header(JwtClaimsForwardingFilter.X_USER_ID, "attacker")
                        .header("Accept", "application/json")
                        .build();
        var mutated =
                request.mutate()
                        .headers(ApiKeyAuthHeaders::stripSpoofableHeaders)
                        .build();
        assertNull(mutated.getHeaders().getFirst(JwtClaimsForwardingFilter.X_USER_ID));
        assertEquals("application/json", mutated.getHeaders().getFirst("Accept"));
    }
}
