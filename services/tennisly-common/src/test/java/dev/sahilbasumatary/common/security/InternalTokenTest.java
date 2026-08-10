package dev.sahilbasumatary.common.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InternalTokenTest {

    @Test
    void blankExpectedDisablesEnforcement() {
        assertTrue(InternalToken.matches("", null));
        assertTrue(InternalToken.matches(null, "anything"));
        assertFalse(InternalToken.isEnabled(""));
    }

    @Test
    void rejectsMissingOrWrongToken() {
        assertFalse(InternalToken.matches("secret", null));
        assertFalse(InternalToken.matches("secret", "wrong"));
        assertTrue(InternalToken.matches("secret", "secret"));
    }
}
