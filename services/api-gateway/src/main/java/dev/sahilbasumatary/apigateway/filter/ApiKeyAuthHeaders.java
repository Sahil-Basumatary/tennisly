package dev.sahilbasumatary.apigateway.filter;

import dev.sahilbasumatary.apigateway.client.ApiKeyValidationResponse;
import dev.sahilbasumatary.apigateway.ratelimit.PlanTierRateLimits;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

public final class ApiKeyAuthHeaders {

    public static final String X_API_KEY = "X-Api-Key";
    public static final String X_ORG_ID = "X-Org-Id";
    public static final String X_API_KEY_ID = "X-Api-Key-Id";
    public static final String X_API_KEY_SCOPES = "X-Api-Key-Scopes";
    public static final String X_PLAN_TIER = "X-Plan-Tier";

    private static final List<String> SPOOFABLE_HEADERS =
            List.of(
                    JwtClaimsForwardingFilter.X_USER_ID,
                    JwtClaimsForwardingFilter.X_ORG_ID,
                    JwtClaimsForwardingFilter.X_USER_ROLES,
                    X_API_KEY_ID,
                    X_API_KEY_SCOPES,
                    X_PLAN_TIER,
                    X_API_KEY);

    private ApiKeyAuthHeaders() {}

    public static void stripSpoofableHeaders(HttpHeaders headers) {
        SPOOFABLE_HEADERS.forEach(headers::remove);
    }

    public static ServerHttpRequest applyTrustedHeaders(
            ServerHttpRequest request, ApiKeyValidationResponse validation) {
        return request.mutate()
                .headers(
                        headers -> {
                            stripSpoofableHeaders(headers);
                            headers.set(X_ORG_ID, validation.organizationId().toString());
                            headers.set(X_API_KEY_ID, validation.apiKeyId().toString());
                            headers.set(X_API_KEY_SCOPES, String.join(",", validation.scopes()));
                            headers.set(
                                    X_PLAN_TIER,
                                    PlanTierRateLimits.normalizeTier(validation.planTier()));
                        })
                .build();
    }
}
