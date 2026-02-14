package dev.sahilbasumatary.authservice.service;

import dev.sahilbasumatary.authservice.exception.WebhookVerificationException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebhookVerificationService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SECRET_PREFIX = "whsec_";
    private static final long TOLERANCE_SECONDS = 300;
    private static final String SIGNATURE_VERSION = "v1";
    private final byte[] secretKey;

    public WebhookVerificationService(
            @Value("${clerk.webhook.secret}") String secret) {
        String raw = secret.startsWith(SECRET_PREFIX)
                ? secret.substring(SECRET_PREFIX.length())
                : secret;
        this.secretKey = Base64.getDecoder().decode(raw);
    }

    public void verify(String payload, String msgId, String msgTimestamp,
            String msgSignature) {
        verifyTimestamp(msgTimestamp);
        String signedContent = msgId + "." + msgTimestamp + "." + payload;
        String expectedSignature = computeSignature(signedContent);
        if (!matchesAnySignature(msgSignature, expectedSignature)) {
            throw new WebhookVerificationException("Invalid webhook signature");
        }
    }

    private void verifyTimestamp(String msgTimestamp) {
        long timestamp;
        try {
            timestamp = Long.parseLong(msgTimestamp);
        } catch (NumberFormatException e) {
            throw new WebhookVerificationException("Invalid timestamp format");
        }
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > TOLERANCE_SECONDS) {
            throw new WebhookVerificationException(
                    "Timestamp outside tolerance window");
        }
    }

    private boolean matchesAnySignature(String msgSignature,
            String expectedSignature) {
        // Svix sends space-separated signatures, each prefixed with version
        String[] signatures = msgSignature.split(" ");
        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        for (String sig : signatures) {
            String[] parts = sig.split(",", 2);
            if (parts.length == 2 && SIGNATURE_VERSION.equals(parts[0])) {
                byte[] actualBytes = parts[1].getBytes(StandardCharsets.UTF_8);
                if (MessageDigest.isEqual(expectedBytes, actualBytes)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String computeSignature(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKey, HMAC_SHA256));
            byte[] hash = mac.doFinal(
                    content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new WebhookVerificationException(
                    "Failed to compute signature", e);
        }
    }
}
