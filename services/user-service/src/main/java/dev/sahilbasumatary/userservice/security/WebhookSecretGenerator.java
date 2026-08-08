package dev.sahilbasumatary.userservice.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class WebhookSecretGenerator {

    private static final String PREFIX = "whsec_";
    private static final int SECRET_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private WebhookSecretGenerator() {}

    public static GeneratedWebhookSecret generate() {
        byte[] secret = new byte[SECRET_BYTES];
        RANDOM.nextBytes(secret);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        String plaintext = PREFIX + encoded;
        String prefix = plaintext.substring(0, Math.min(16, plaintext.length()));
        String hash = sha256(plaintext);
        return new GeneratedWebhookSecret(plaintext, prefix, hash);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record GeneratedWebhookSecret(String plaintext, String prefix, String hash) {}
}
