package dev.sahilbasumatary.common.notification;

import java.util.Map;
import java.util.Set;

/** Shared alert categories for email and push channels. */
public final class NotificationCategories {

    public static final String WELCOME = "welcome";
    public static final String API_KEY_REVOKED = "apiKeyRevoked";
    public static final String WEBHOOK_FAILED = "webhookFailed";
    public static final String EMAIL_EXTRA_KEY = "emailCategories";
    public static final String PUSH_EXTRA_KEY = "pushCategories";

    private static final Set<String> ALL = Set.of(WELCOME, API_KEY_REVOKED, WEBHOOK_FAILED);

    private NotificationCategories() {}

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
