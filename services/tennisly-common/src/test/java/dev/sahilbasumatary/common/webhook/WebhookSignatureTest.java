package dev.sahilbasumatary.common.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class WebhookSignatureTest {

    private static final String SECRET = "whsec_test_secret";
    private static final String BODY = "{\"event\":\"match.completed\"}";
    private static final long TS = 1_700_000_000L;

    @Test
    void signAndVerifyRoundTrip() {
        String header = WebhookSignature.sign(SECRET, TS, BODY);
        assertTrue(
                WebhookSignature.verify(
                        SECRET, header, BODY.getBytes(StandardCharsets.UTF_8), TS));
    }

    @Test
    void verifyRejectsWrongSecret() {
        String header = WebhookSignature.sign(SECRET, TS, BODY);
        assertFalse(
                WebhookSignature.verify(
                        "whsec_other", header, BODY.getBytes(StandardCharsets.UTF_8), TS));
    }

    @Test
    void verifyRejectsTamperedBody() {
        String header = WebhookSignature.sign(SECRET, TS, BODY);
        assertFalse(
                WebhookSignature.verify(
                        SECRET,
                        header,
                        "{\"event\":\"match.point_recorded\"}".getBytes(StandardCharsets.UTF_8),
                        TS));
    }

    @Test
    void verifyRejectsStaleTimestamp() {
        String header = WebhookSignature.sign(SECRET, TS, BODY);
        long now = TS + WebhookSignature.DEFAULT_TOLERANCE_SECONDS + 1;
        assertFalse(
                WebhookSignature.verify(
                        SECRET, header, BODY.getBytes(StandardCharsets.UTF_8), now));
    }

    @Test
    void verifyRejectsMalformedHeader() {
        assertFalse(
                WebhookSignature.verify(
                        SECRET, "not-a-signature", BODY.getBytes(StandardCharsets.UTF_8), TS));
        assertFalse(
                WebhookSignature.verify(SECRET, null, BODY.getBytes(StandardCharsets.UTF_8), TS));
    }

    @Test
    void signatureHexIsSha256Length() {
        String header = WebhookSignature.sign(SECRET, TS, BODY);
        String hex = header.substring(header.indexOf("v1=") + 3);
        assertEquals(64, hex.length());
    }
}
