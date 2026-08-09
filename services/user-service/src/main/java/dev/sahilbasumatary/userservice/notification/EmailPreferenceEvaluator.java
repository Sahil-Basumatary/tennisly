package dev.sahilbasumatary.userservice.notification;

import dev.sahilbasumatary.common.notification.NotificationCategories;
import dev.sahilbasumatary.userservice.entity.UserPreference;
import java.util.HashMap;
import java.util.Map;

public final class EmailPreferenceEvaluator {

    private EmailPreferenceEvaluator() {}

    public static boolean isCategoryEnabled(UserPreference preference, String category) {
        return isChannelCategoryEnabled(
                preference,
                preference != null && preference.isEmailNotifications(),
                NotificationCategories.EMAIL_EXTRA_KEY,
                category);
    }

    public static boolean isPushCategoryEnabled(UserPreference preference, String category) {
        return isChannelCategoryEnabled(
                preference,
                preference != null && preference.isPushNotifications(),
                NotificationCategories.PUSH_EXTRA_KEY,
                category);
    }

    private static boolean isChannelCategoryEnabled(
            UserPreference preference,
            boolean channelEnabled,
            String extraKey,
            String category) {
        if (preference == null || category == null || category.isBlank()) {
            return false;
        }
        if (!preference.isNotificationsEnabled() || !channelEnabled) {
            return false;
        }
        Object raw =
                preference.getExtraSettings() == null
                        ? null
                        : preference.getExtraSettings().get(extraKey);
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
        extra.put(
                NotificationCategories.EMAIL_EXTRA_KEY,
                new HashMap<>(NotificationCategories.defaultCategories()));
        extra.put(
                NotificationCategories.PUSH_EXTRA_KEY,
                new HashMap<>(NotificationCategories.defaultCategories()));
        return extra;
    }
}
