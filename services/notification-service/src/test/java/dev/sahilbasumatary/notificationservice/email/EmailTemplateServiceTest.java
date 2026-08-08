package dev.sahilbasumatary.notificationservice.email;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailTemplateServiceTest {

    private final EmailTemplateService templates = new EmailTemplateService();

    @Test
    void welcomeRendersSubjectAndBodies() {
        EmailMessage message = templates.welcome("a@example.com", "Sahil");
        assertTrue(message.subject().contains("Welcome"));
        assertTrue(message.textBody().contains("Sahil"));
        assertTrue(message.htmlBody().contains("Tennisly"));
        assertFalse(message.htmlBody().contains("<script"));
    }

    @Test
    void apiKeyRevokedEscapesHtml() {
        EmailMessage message = templates.apiKeyRevoked("a@example.com", "Ops", "<bad>");
        assertTrue(message.htmlBody().contains("&lt;bad&gt;"));
        assertFalse(message.htmlBody().contains("<bad>"));
    }
}
