package dev.sahilbasumatary.notificationservice.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FCM HTTP v1 sender. Expects a short-lived OAuth access token via config — production should mint
 * this from a service account / workload identity outside the app process.
 */
public class FcmHttpV1PushSender implements PushSender {

    private final String projectId;
    private final String accessToken;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public FcmHttpV1PushSender(String projectId, String accessToken, ObjectMapper objectMapper) {
        this.projectId = projectId;
        this.accessToken = accessToken;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public void send(PushMessage message) {
        try {
            Map<String, Object> notification = new LinkedHashMap<>();
            notification.put("title", message.title());
            notification.put("body", message.body());
            Map<String, Object> fcmMessage = new LinkedHashMap<>();
            fcmMessage.put("token", message.token());
            fcmMessage.put("notification", notification);
            if (message.data() != null && !message.data().isEmpty()) {
                fcmMessage.put("data", message.data());
            }
            Map<String, Object> body = Map.of("message", fcmMessage);
            byte[] payload = objectMapper.writeValueAsBytes(body);
            URI uri =
                    URI.create(
                            "https://fcm.googleapis.com/v1/projects/"
                                    + projectId
                                    + "/messages:send");
            HttpRequest request =
                    HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofSeconds(15))
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                            .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "FCM HTTP " + response.statusCode() + ": " + truncate(response.body()));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FCM send interrupted", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("FCM send failed: " + ex.getMessage(), ex);
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 200 ? body : body.substring(0, 200);
    }
}
