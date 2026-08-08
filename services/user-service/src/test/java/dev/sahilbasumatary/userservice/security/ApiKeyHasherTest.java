package dev.sahilbasumatary.userservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApiKeyHasherTest {

    @Test
    void hashIsDeterministicSha256Hex() {
        String hash1 = ApiKeyHasher.hash("tly_live_test");
        String hash2 = ApiKeyHasher.hash("tly_live_test");
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
        assertTrue(hash1.matches("[0-9a-f]{64}"));
    }

    @Test
    void hashDiffersForDifferentKeys() {
        assertNotEquals(ApiKeyHasher.hash("tly_live_a"), ApiKeyHasher.hash("tly_live_b"));
    }
}
