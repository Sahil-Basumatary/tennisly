package dev.sahilbasumatary.common.event;

import java.util.Set;

public final class WebhookEventTypes {

    private WebhookEventTypes() {}

    public static final String MATCH_COMPLETED = "match.completed";
    public static final String MATCH_POINT_RECORDED = "match.point_recorded";
    public static final String API_KEY_REVOKED = "api_key.revoked";
    public static final String WEBHOOK_TEST = "webhook.test";

    private static final Set<String> ALL = Set.of(
            MATCH_COMPLETED,
            MATCH_POINT_RECORDED,
            API_KEY_REVOKED,
            WEBHOOK_TEST);

    public static Set<String> all() {
        return ALL;
    }

    public static boolean isValid(String eventType) {
        return ALL.contains(eventType);
    }
}
