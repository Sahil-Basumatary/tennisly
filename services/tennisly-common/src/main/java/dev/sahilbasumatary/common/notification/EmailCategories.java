package dev.sahilbasumatary.common.notification;

import java.util.Map;
import java.util.Set;

public final class EmailCategories {

    public static final String WELCOME = "welcome";
    public static final String API_KEY_REVOKED = "apiKeyRevoked";
    public static final String WEBHOOK_FAILED = "webhookFailed";
    public static final String EXTRA_SETTINGS_KEY = "emailCategories";

    private static final Set<String> ALL = Set.of(WELCOME, API_KEY_REVOKED, WEBHOOK_FAILED);

    private EmailCategories() {}

    public static Set<String> all() {
        return ALL;
    }

    public static boolean isValid(String category) {
        return ALL.contains(category);
    }

    public static Map<String, Boolean> defaultCategories() {
        return Map.of(
                WELCOME, true,
                API_KEY_REVOKED, true,
                WEBHOOK_FAILED, true);
    }
}
