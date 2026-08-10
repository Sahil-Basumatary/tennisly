package dev.sahilbasumatary.userservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WebhookSecretGeneratorTest {

    @Test
    void generatesPrefixedSecretWithMatchingHash() {
        WebhookSecretGenerator.GeneratedWebhookSecret generated = WebhookSecretGenerator.generate();
        assertTrue(generated.plaintext().startsWith("whsec_"));
        assertEquals(16, generated.prefix().length());
        assertEquals(generated.prefix(), generated.plaintext().substring(0, 16));
        assertEquals(ApiKeyHasher.hash(generated.plaintext()), generated.hash());
    }

    @Test
    void generatesUniqueSecrets() {
        WebhookSecretGenerator.GeneratedWebhookSecret first = WebhookSecretGenerator.generate();
        WebhookSecretGenerator.GeneratedWebhookSecret second = WebhookSecretGenerator.generate();
        assertNotEquals(first.plaintext(), second.plaintext());
        assertNotEquals(first.hash(), second.hash());
    }
}
