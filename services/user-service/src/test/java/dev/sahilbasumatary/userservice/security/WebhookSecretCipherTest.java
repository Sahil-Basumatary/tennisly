package dev.sahilbasumatary.userservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WebhookSecretCipherTest {

    private static final String DEV_KEY_BASE64 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Test
    void encryptDecryptRoundtrip() {
        WebhookSecretCipher cipher = new WebhookSecretCipher(DEV_KEY_BASE64);
        String plaintext = "whsec_abcdef1234567890";
        String encrypted = cipher.encrypt(plaintext);
        String decrypted = cipher.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encryptProducesDifferentCiphertextEachTime() {
        WebhookSecretCipher cipher = new WebhookSecretCipher(DEV_KEY_BASE64);
        String plaintext = "whsec_test_secret";
        String encrypted1 = cipher.encrypt(plaintext);
        String encrypted2 = cipher.encrypt(plaintext);
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void decryptWithWrongKeyFails() {
        WebhookSecretCipher cipher1 = new WebhookSecretCipher(DEV_KEY_BASE64);
        String encrypted = cipher1.encrypt("whsec_secret");
        String differentKey = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=";
        WebhookSecretCipher cipher2 = new WebhookSecretCipher(differentKey);
        assertThrows(IllegalStateException.class, () -> cipher2.decrypt(encrypted));
    }

    @Test
    void rejectsInvalidKeyLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new WebhookSecretCipher("dG9vc2hvcnQ="));
    }

    @Test
    void handlesEmptyPlaintext() {
        WebhookSecretCipher cipher = new WebhookSecretCipher(DEV_KEY_BASE64);
        String encrypted = cipher.encrypt("");
        String decrypted = cipher.decrypt(encrypted);
        assertEquals("", decrypted);
    }
}
