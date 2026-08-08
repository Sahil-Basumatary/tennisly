package dev.sahilbasumatary.userservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApiKeyGeneratorTest {

    @Test
    void generatesPrefixedUrlSafeKey() {
        ApiKeyGenerator.GeneratedApiKey generated = ApiKeyGenerator.generate();
        assertTrue(generated.plaintext().startsWith("tly_live_"));
        assertEquals(16, generated.prefix().length());
        assertEquals(generated.prefix(), generated.plaintext().substring(0, 16));
        assertEquals(ApiKeyHasher.hash(generated.plaintext()), generated.hash());
    }

    @Test
    void generatesUniqueKeys() {
        ApiKeyGenerator.GeneratedApiKey first = ApiKeyGenerator.generate();
        ApiKeyGenerator.GeneratedApiKey second = ApiKeyGenerator.generate();
        assertNotEquals(first.plaintext(), second.plaintext());
        assertNotEquals(first.hash(), second.hash());
    }
}
