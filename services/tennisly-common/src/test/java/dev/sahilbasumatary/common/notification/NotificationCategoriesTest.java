package dev.sahilbasumatary.common.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NotificationCategoriesTest {

    @Test
    void allKnownCategoriesAreValid() {
        assertTrue(NotificationCategories.isValid(NotificationCategories.WELCOME));
        assertTrue(NotificationCategories.isValid(NotificationCategories.API_KEY_REVOKED));
        assertTrue(NotificationCategories.isValid(NotificationCategories.WEBHOOK_FAILED));
        assertFalse(NotificationCategories.isValid("unknown"));
        assertEquals(3, NotificationCategories.all().size());
        assertEquals(3, NotificationCategories.defaultCategories().size());
    }

    @Test
    void emailCategoriesDelegateToSharedSet() {
        assertEquals(NotificationCategories.all(), EmailCategories.all());
        assertTrue(EmailCategories.isValid(EmailCategories.WELCOME));
        assertEquals(
                NotificationCategories.defaultCategories(), EmailCategories.defaultCategories());
        assertEquals(NotificationCategories.EMAIL_EXTRA_KEY, EmailCategories.EXTRA_SETTINGS_KEY);
    }
}
