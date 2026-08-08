package dev.sahilbasumatary.notificationservice.email;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateService {

    public EmailMessage welcome(String to, String displayName) {
        String name = blankToFallback(displayName, "there");
        String subject = "Welcome to Tennisly";
        String text =
                "Hi "
                        + name
                        + ",\n\n"
                        + "Your Tennisly account is ready. Watch live scores, dig into tape-backed analytics,"
                        + " and wire partner webhooks when you need them.\n\n"
                        + "— Tennisly\n";
        String html =
                baseHtml(
                        subject,
                        "<p>Hi "
                                + escape(name)
                                + ",</p>"
                                + "<p>Your Tennisly account is ready. Watch live scores, dig into"
                                + " tape-backed analytics, and wire partner webhooks when you need"
                                + " them.</p>");
        return new EmailMessage(to, subject, html, text);
    }

    public EmailMessage apiKeyRevoked(String to, String displayName, String keyPrefix) {
        String name = blankToFallback(displayName, "there");
        String subject = "Tennisly API key revoked";
        String prefix = blankToFallback(keyPrefix, "tly_live_");
        String text =
                "Hi "
                        + name
                        + ",\n\n"
                        + "An API key starting with "
                        + prefix
                        + " was revoked on your organization.\n"
                        + "If this wasn't expected, rotate credentials and review admin audit logs.\n\n"
                        + "— Tennisly\n";
        String html =
                baseHtml(
                        subject,
                        "<p>Hi "
                                + escape(name)
                                + ",</p>"
                                + "<p>An API key starting with <code>"
                                + escape(prefix)
                                + "</code> was revoked on your organization.</p>"
                                + "<p>If this wasn't expected, rotate credentials and review admin"
                                + " audit logs.</p>");
        return new EmailMessage(to, subject, html, text);
    }

    public EmailMessage webhookFailed(
            String to, String displayName, String eventType, String lastError) {
        String name = blankToFallback(displayName, "there");
        String subject = "Tennisly webhook delivery exhausted retries";
        String err = blankToFallback(lastError, "unknown error");
        String text =
                "Hi "
                        + name
                        + ",\n\n"
                        + "A webhook delivery for event type "
                        + eventType
                        + " exhausted retries and was marked DEAD.\n"
                        + "Last error: "
                        + err
                        + "\n"
                        + "Requeue from Admin → Webhooks → Delivery log if the endpoint is healthy"
                        + " again.\n\n"
                        + "— Tennisly\n";
        String html =
                baseHtml(
                        subject,
                        "<p>Hi "
                                + escape(name)
                                + ",</p>"
                                + "<p>A webhook delivery for <code>"
                                + escape(eventType)
                                + "</code> exhausted retries and was marked DEAD.</p>"
                                + "<p>Last error: "
                                + escape(err)
                                + "</p>"
                                + "<p>Requeue from Admin → Webhooks → Delivery log if the endpoint is"
                                + " healthy again.</p>");
        return new EmailMessage(to, subject, html, text);
    }

    private static String baseHtml(String title, String body) {
        return "<!DOCTYPE html><html><body style=\"font-family:Georgia,serif;color:#111;"
                + "background:#f7f5f1;padding:24px;\">"
                + "<div style=\"max-width:560px;margin:0 auto;background:#fff;padding:28px;"
                + "border:1px solid #ddd;\">"
                + "<p style=\"letter-spacing:0.16em;text-transform:uppercase;font-size:11px;"
                + "color:#666;margin:0 0 16px;\">Tennisly</p>"
                + "<h1 style=\"font-size:22px;margin:0 0 16px;\">"
                + escape(title)
                + "</h1>"
                + body
                + "<p style=\"margin-top:28px;font-size:12px;color:#777;\">— Tennisly</p>"
                + "</div></body></html>";
    }

    private static String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
