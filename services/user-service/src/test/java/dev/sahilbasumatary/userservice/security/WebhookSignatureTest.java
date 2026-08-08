package dev.sahilbasumatary.userservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.common.webhook.WebhookSignature;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class WebhookSignatureTest {

    private static final String SECRET = "whsec_test_secret_key_for_unit_tests";
    private static final byte[] BODY = "{\"event\":\"test\"}".getBytes(StandardCharsets.UTF_8);
    private static final long TIMESTAMP = 1700000000L;

    @Test
    void signProducesExpectedFormat() {
        String signature = WebhookSignature.sign(SECRET, TIMESTAMP, BODY);
        assertTrue(signature.startsWith("t=" + TIMESTAMP + ",v1="));
        assertTrue(signature.length() > 20);
    }

    @Test
    void verifyAcceptsValidSignature() {
        String signature = WebhookSignature.sign(SECRET, TIMESTAMP, BODY);
        boolean valid = WebhookSignature.verify(SECRET, signature, BODY, TIMESTAMP);
        assertTrue(valid);
    }

    @Test
    void verifyRejectsWrongSecret() {
        String signature = WebhookSignature.sign(SECRET, TIMESTAMP, BODY);
        boolean valid = WebhookSignature.verify("wrong_secret", signature, BODY, TIMESTAMP);
        assertFalse(valid);
    }

    @Test
    void verifyRejectsTamperedBody() {
        String signature = WebhookSignature.sign(SECRET, TIMESTAMP, BODY);
        byte[] tampered = "{\"event\":\"hacked\"}".getBytes(StandardCharsets.UTF_8);
        boolean valid = WebhookSignature.verify(SECRET, signature, tampered, TIMESTAMP);
        assertFalse(valid);
    }

    @Test
    void verifyRejectsExpiredTimestamp() {
        String signature = WebhookSignature.sign(SECRET, TIMESTAMP, BODY);
        long futureTime = TIMESTAMP + 600;
        boolean valid = WebhookSignature.verify(SECRET, signature, BODY, futureTime);
        assertFalse(valid);
    }

    @Test
    void verifyAcceptsWithinToleranceWindow() {
        String signature = WebhookSignature.sign(SECRET, TIMESTAMP, BODY);
        long nearFuture = TIMESTAMP + 200;
        boolean valid = WebhookSignature.verify(SECRET, signature, BODY, nearFuture);
        assertTrue(valid);
    }

    @Test
    void signatureIsDeterministicForSameInputs() {
        String sig1 = WebhookSignature.sign(SECRET, TIMESTAMP, BODY);
        String sig2 = WebhookSignature.sign(SECRET, TIMESTAMP, BODY);
        assertEquals(sig1, sig2);
    }

    @Test
    void verifyRejectsNullHeader() {
        assertFalse(WebhookSignature.verify(SECRET, null, BODY, TIMESTAMP));
    }

    @Test
    void verifyRejectsMalformedHeader() {
        assertFalse(WebhookSignature.verify(SECRET, "garbage", BODY, TIMESTAMP));
    }
}
