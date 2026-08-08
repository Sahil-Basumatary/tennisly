package dev.sahilbasumatary.common.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class WebhookSignature {

    private static final String HMAC_ALGO = "HmacSHA256";
    public static final long DEFAULT_TOLERANCE_SECONDS = 300L;

    private WebhookSignature() {}

    public static String sign(String secret, long timestampEpochSecond, String body) {
        return sign(secret, timestampEpochSecond, body.getBytes(StandardCharsets.UTF_8));
    }

    public static String sign(String secret, long timestampEpochSecond, byte[] body) {
        String signedContent = timestampEpochSecond + "." + new String(body, StandardCharsets.UTF_8);
        byte[] hash = hmacSha256(secret.getBytes(StandardCharsets.UTF_8),
                signedContent.getBytes(StandardCharsets.UTF_8));
        return "t=" + timestampEpochSecond + ",v1=" + hex(hash);
    }

    public static String sign(String secret, String body) {
        return sign(secret, Instant.now().getEpochSecond(), body);
    }

    public static boolean verify(
            String secret, String header, byte[] body, long nowEpochSecond) {
        return verify(secret, header, body, nowEpochSecond, DEFAULT_TOLERANCE_SECONDS);
    }

    public static boolean verify(
            String secret,
            String header,
            byte[] body,
            long nowEpochSecond,
            long toleranceSeconds) {
        if (secret == null || header == null || body == null || header.isBlank()) {
            return false;
        }
        Long timestamp = null;
        String v1 = null;
        for (String part : header.split(",")) {
            String trimmed = part.trim();
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eq);
            String value = trimmed.substring(eq + 1);
            if ("t".equals(key)) {
                try {
                    timestamp = Long.parseLong(value);
                } catch (NumberFormatException ex) {
                    return false;
                }
            } else if ("v1".equals(key)) {
                v1 = value;
            }
        }
        if (timestamp == null || v1 == null || v1.isBlank()) {
            return false;
        }
        if (Math.abs(nowEpochSecond - timestamp) > toleranceSeconds) {
            return false;
        }
        String expected = sign(secret, timestamp, body);
        String expectedV1 = expected.substring(expected.indexOf("v1=") + 3);
        return MessageDigest.isEqual(
                expectedV1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                v1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(key, HMAC_ALGO));
            return mac.doFinal(data);
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", ex);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
