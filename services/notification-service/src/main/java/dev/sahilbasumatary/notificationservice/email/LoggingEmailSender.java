package dev.sahilbasumatary.notificationservice.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info(
                "email[logging] to={} subject={} textChars={}",
                message.to(),
                message.subject(),
                message.textBody() == null ? 0 : message.textBody().length());
    }
}
