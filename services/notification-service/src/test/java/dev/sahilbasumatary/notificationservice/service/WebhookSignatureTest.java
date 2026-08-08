package dev.sahilbasumatary.notificationservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.common.webhook.WebhookSignature;
import org.junit.jupiter.api.Test;

class WebhookSignatureTest {

    @Test
    void signatureContainsTimestampAndV1() {
        String sig = WebhookSignature.sign(
                "whsec_test123", 1700000000L, "{\"hello\":\"world\"}");
        assertTrue(sig.startsWith("t=1700000000,v1="));
        String hex = sig.substring(sig.indexOf("v1=") + 3);
        assertEquals(64, hex.length(), "SHA-256 hex should be 64 chars");
    }

    @Test
    void signatureIsDeterministic() {
        String body = "{\"id\":\"abc\"}";
        String sig1 = WebhookSignature.sign("secret", 1234L, body);
        String sig2 = WebhookSignature.sign("secret", 1234L, body);
        assertEquals(sig1, sig2);
    }

    @Test
    void differentSecretProducesDifferentSignature() {
        String body = "{\"test\":true}";
        String sig1 = WebhookSignature.sign("secret-a", 1234L, body);
        String sig2 = WebhookSignature.sign("secret-b", 1234L, body);
        assertNotEquals(sig1, sig2);
    }

    @Test
    void convenienceSignUsesCurrentTimestamp() {
        String sig = WebhookSignature.sign("secret", "{\"data\":1}");
        assertNotNull(sig);
        assertTrue(sig.startsWith("t="));
        assertTrue(sig.contains(",v1="));
    }
}
