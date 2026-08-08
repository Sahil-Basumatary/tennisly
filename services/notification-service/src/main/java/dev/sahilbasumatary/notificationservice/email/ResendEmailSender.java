package dev.sahilbasumatary.notificationservice.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResendEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);
    private static final URI RESEND_URI = URI.create("https://api.resend.com/emails");

    private final String apiKey;
    private final String from;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ResendEmailSender(String apiKey, String from, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.from = from;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public void send(EmailMessage message) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", from);
            body.put("to", List.of(message.to()));
            body.put("subject", message.subject());
            body.put("html", message.htmlBody());
            body.put("text", message.textBody());
            byte[] payload = objectMapper.writeValueAsBytes(body);
            HttpRequest request =
                    HttpRequest.newBuilder(RESEND_URI)
                            .timeout(Duration.ofSeconds(15))
                            .header("Authorization", "Bearer " + apiKey)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                            .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Resend HTTP " + response.statusCode() + ": " + truncate(response.body()));
            }
            log.info("email[resend] accepted to={} subject={}", message.to(), message.subject());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Resend send interrupted", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Resend send failed: " + ex.getMessage(), ex);
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 200 ? body : body.substring(0, 200);
    }
}
