package dev.sahilbasumatary.userservice.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WebhookUrlValidatorTest {

    @Test
    void acceptsHttpsPublicUrl() {
        WebhookUrlValidator validator = new WebhookUrlValidator(false);
        assertDoesNotThrow(() -> validator.validate("https://203.0.113.50/webhook"));
    }

    @Test
    void rejectsHttpInProductionMode() {
        WebhookUrlValidator validator = new WebhookUrlValidator(false);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("http://203.0.113.50/webhook"));
    }

    @Test
    void allowsHttpWhenPrivateTargetsEnabled() {
        WebhookUrlValidator validator = new WebhookUrlValidator(true);
        assertDoesNotThrow(() -> validator.validate("http://localhost:8080/webhook"));
    }

    @Test
    void rejectsEmptyUrl() {
        WebhookUrlValidator validator = new WebhookUrlValidator(true);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(""));
    }

    @Test
    void rejectsNullUrl() {
        WebhookUrlValidator validator = new WebhookUrlValidator(true);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(null));
    }

    @Test
    void rejectsFtpScheme() {
        WebhookUrlValidator validator = new WebhookUrlValidator(true);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("ftp://example.com/file"));
    }

    @Test
    void rejectsUrlWithoutHost() {
        WebhookUrlValidator validator = new WebhookUrlValidator(true);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https:///path"));
    }

    @Test
    void rejectsLocalhostInProductionMode() {
        WebhookUrlValidator validator = new WebhookUrlValidator(false);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://localhost/webhook"));
    }

    @Test
    void rejectsMetadataEndpoint() {
        WebhookUrlValidator validator = new WebhookUrlValidator(false);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://169.254.169.254/latest/meta-data/"));
    }

    @Test
    void rejectsPrivateNetworkInProductionMode() {
        WebhookUrlValidator validator = new WebhookUrlValidator(false);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://192.168.1.1/webhook"));
    }
}
