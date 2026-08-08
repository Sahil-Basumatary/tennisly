package dev.sahilbasumatary.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.notificationservice.email.EmailSender;
import dev.sahilbasumatary.notificationservice.email.LoggingEmailSender;
import dev.sahilbasumatary.notificationservice.email.ResendEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    @Bean
    EmailSender emailSender(EmailProperties properties, ObjectMapper objectMapper) {
        String provider = properties.provider() == null ? "logging" : properties.provider().trim();
        boolean resendReady =
                "resend".equalsIgnoreCase(provider)
                        && properties.resendApiKey() != null
                        && !properties.resendApiKey().isBlank();
        if (resendReady) {
            log.info("Email provider=resend from={}", properties.from());
            return new ResendEmailSender(properties.resendApiKey(), properties.from(), objectMapper);
        }
        if ("resend".equalsIgnoreCase(provider)) {
            log.warn("RESEND_API_KEY missing — falling back to logging email provider");
        } else {
            log.info("Email provider=logging");
        }
        return new LoggingEmailSender();
    }
}
