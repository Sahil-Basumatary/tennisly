package dev.sahilbasumatary.userservice.notification;

import dev.sahilbasumatary.common.notification.EmailCategories;
import dev.sahilbasumatary.userservice.entity.UserPreference;
import java.util.HashMap;
import java.util.Map;

public final class EmailPreferenceEvaluator {

    private EmailPreferenceEvaluator() {}

    public static boolean isCategoryEnabled(UserPreference preference, String category) {
        if (preference == null || category == null || category.isBlank()) {
            return false;
        }
        if (!preference.isNotificationsEnabled() || !preference.isEmailNotifications()) {
            return false;
        }
        Object raw = preference.getExtraSettings() == null
                ? null
                : preference.getExtraSettings().get(EmailCategories.EXTRA_SETTINGS_KEY);
        if (!(raw instanceof Map<?, ?> categories)) {
            return true;
        }
        Object value = categories.get(category);
        if (value == null) {
            return true;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public static Map<String, Object> seededExtraSettings() {
        Map<String, Object> extra = new HashMap<>();
        extra.put(EmailCategories.EXTRA_SETTINGS_KEY, new HashMap<>(EmailCategories.defaultCategories()));
        return extra;
    }
}
