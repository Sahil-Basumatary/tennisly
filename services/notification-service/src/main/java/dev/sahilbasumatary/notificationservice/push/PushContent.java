package dev.sahilbasumatary.notificationservice.push;

import java.util.Map;

public record PushContent(String title, String body, Map<String, String> data) {}
