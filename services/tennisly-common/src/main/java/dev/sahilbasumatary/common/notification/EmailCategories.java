package dev.sahilbasumatary.common.notification;

import java.util.Map;
import java.util.Set;

/**
 * Email-specific view of the shared notification categories.
 *
 * @deprecated Prefer {@link NotificationCategories}; kept for existing email call sites.
 */
@Deprecated
public final class EmailCategories {

    public static final String WELCOME = NotificationCategories.WELCOME;
    public static final String API_KEY_REVOKED = NotificationCategories.API_KEY_REVOKED;
    public static final String WEBHOOK_FAILED = NotificationCategories.WEBHOOK_FAILED;
    public static final String EXTRA_SETTINGS_KEY = NotificationCategories.EMAIL_EXTRA_KEY;

    private EmailCategories() {}

    public static Set<String> all() {
        return NotificationCategories.all();
    }

    public static boolean isValid(String category) {
        return NotificationCategories.isValid(category);
    }

    public static Map<String, Boolean> defaultCategories() {
        return NotificationCategories.defaultCategories();
    }
}
