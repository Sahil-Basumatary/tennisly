package dev.sahilbasumatary.apigateway.ratelimit;

import dev.sahilbasumatary.apigateway.config.PlanTierRateLimitProperties;

public final class PlanTierRateLimits {

    private PlanTierRateLimits() {}

    public static String normalizeTier(String planTier) {
        if (planTier == null || planTier.isBlank()) {
            return "FREE";
        }
        return planTier.trim().toUpperCase();
    }

    public static int requestsPerMinute(String planTier, PlanTierRateLimitProperties limits) {
        return switch (normalizeTier(planTier)) {
            case "BASIC" -> limits.getBasic();
            case "PRO" -> limits.getPro();
            case "ENTERPRISE" -> limits.getEnterprise();
            default -> limits.getFree();
        };
    }
}
