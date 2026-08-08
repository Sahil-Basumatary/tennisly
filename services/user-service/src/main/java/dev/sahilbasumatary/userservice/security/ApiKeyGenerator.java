package dev.sahilbasumatary.userservice.security;

import java.security.SecureRandom;
import java.util.Base64;

public final class ApiKeyGenerator {

    private static final String PREFIX = "tly_live_";
    private static final int SECRET_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiKeyGenerator() {}

    public static GeneratedApiKey generate() {
        byte[] secret = new byte[SECRET_BYTES];
        RANDOM.nextBytes(secret);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        String plaintext = PREFIX + encoded;
        String prefix = plaintext.substring(0, Math.min(16, plaintext.length()));
        return new GeneratedApiKey(plaintext, prefix, ApiKeyHasher.hash(plaintext));
    }

    public record GeneratedApiKey(String plaintext, String prefix, String hash) {}
}
