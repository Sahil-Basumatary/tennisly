package dev.sahilbasumatary.common.observability;

import java.util.UUID;

public final class RequestIds {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TRACEPARENT_HEADER = "traceparent";
    public static final String MDC_KEY = "requestId";

    private RequestIds() {}

    public static String resolve(String requestId, String traceparent) {
        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }
        String fromTrace = traceIdFromParent(traceparent);
        if (fromTrace != null) {
            return fromTrace;
        }
        return UUID.randomUUID().toString();
    }

    public static String traceIdFromParent(String traceparent) {
        if (traceparent == null || traceparent.isBlank()) {
            return null;
        }
        String[] parts = traceparent.trim().split("-");
        if (parts.length >= 2 && parts[1].length() >= 16) {
            return parts[1];
        }
        return null;
    }
}
