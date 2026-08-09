package dev.sahilbasumatary.notificationservice.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public void send(PushMessage message) {
        log.info(
                "push[logging] tokenSuffix={} title={} bodyChars={}",
                suffix(message.token()),
                message.title(),
                message.body() == null ? 0 : message.body().length());
    }

    private static String suffix(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return "…" + token.substring(token.length() - 8);
    }
}
