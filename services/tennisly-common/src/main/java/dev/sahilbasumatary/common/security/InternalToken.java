package dev.sahilbasumatary.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Shared-secret header between the public gateway (or sibling services) and
 * backends that sit on public Render URLs because private services are paid.
 */
public final class InternalToken {

    public static final String HEADER = "X-Gateway-Token";

    private InternalToken() {}

    public static boolean isEnabled(String expected) {
        return expected != null && !expected.isBlank();
    }

    public static boolean matches(String expected, String provided) {
        if (!isEnabled(expected)) {
            return true;
        }
        if (provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
