package dev.sahilbasumatary.userservice.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebhookUrlValidator {

    private final boolean allowPrivateTargets;

    public WebhookUrlValidator(
            @Value("${tennisly.webhooks.allow-private-targets:true}") boolean allowPrivateTargets) {
        this.allowPrivateTargets = allowPrivateTargets;
    }

    public void validate(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Webhook target URL must not be empty");
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid webhook URL: " + e.getMessage());
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalArgumentException("Webhook URL must use http or https scheme");
        }
        if (!allowPrivateTargets && !"https".equals(scheme)) {
            throw new IllegalArgumentException("Webhook URL must use https in production");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Webhook URL must include a valid host");
        }
        if (!allowPrivateTargets) {
            validateNotPrivate(host);
        }
    }

    private void validateNotPrivate(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress()) {
                throw new IllegalArgumentException("Webhook URL must not target loopback address");
            }
            if (address.isLinkLocalAddress()) {
                throw new IllegalArgumentException("Webhook URL must not target link-local address");
            }
            if (address.isSiteLocalAddress()) {
                throw new IllegalArgumentException("Webhook URL must not target private network address");
            }
            byte[] octets = address.getAddress();
            if (octets.length == 4
                    && (octets[0] & 0xFF) == 169
                    && (octets[1] & 0xFF) == 254) {
                throw new IllegalArgumentException(
                        "Webhook URL must not target metadata endpoint (169.254.x.x)");
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve webhook URL host: " + host);
        }
    }
}
