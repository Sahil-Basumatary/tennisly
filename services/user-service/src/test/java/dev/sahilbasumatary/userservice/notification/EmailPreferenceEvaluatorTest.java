package dev.sahilbasumatary.userservice.notification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.common.notification.EmailCategories;
import dev.sahilbasumatary.userservice.entity.UserPreference;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmailPreferenceEvaluatorTest {

    @Test
    void masterFlagsGateAllCategories() {
        UserPreference preference = seeded();
        preference.setEmailNotifications(false);
        assertFalse(EmailPreferenceEvaluator.isCategoryEnabled(preference, EmailCategories.WELCOME));
    }

    @Test
    void missingCategoryDefaultsToTrue() {
        UserPreference preference = seeded();
        @SuppressWarnings("unchecked")
        Map<String, Object> categories =
                (Map<String, Object>) preference.getExtraSettings().get(EmailCategories.EXTRA_SETTINGS_KEY);
        categories.remove(EmailCategories.WELCOME);
        assertTrue(EmailPreferenceEvaluator.isCategoryEnabled(preference, EmailCategories.WELCOME));
    }

    @Test
    void explicitFalseDisablesCategory() {
        UserPreference preference = seeded();
        @SuppressWarnings("unchecked")
        Map<String, Object> categories =
                (Map<String, Object>) preference.getExtraSettings().get(EmailCategories.EXTRA_SETTINGS_KEY);
        categories.put(EmailCategories.API_KEY_REVOKED, false);
        assertFalse(
                EmailPreferenceEvaluator.isCategoryEnabled(
                        preference, EmailCategories.API_KEY_REVOKED));
    }

    @Test
    void pushMasterFlagGatesCategories() {
        UserPreference preference = seeded();
        preference.setPushNotifications(false);
        assertFalse(
                EmailPreferenceEvaluator.isPushCategoryEnabled(
                        preference, EmailCategories.WELCOME));
    }

    private static UserPreference seeded() {
        UserPreference preference = new UserPreference();
        preference.setNotificationsEnabled(true);
        preference.setEmailNotifications(true);
        preference.setExtraSettings(EmailPreferenceEvaluator.seededExtraSettings());
        return preference;
    }
}
