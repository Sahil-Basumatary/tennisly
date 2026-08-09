package dev.sahilbasumatary.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.notificationservice.push.FcmHttpV1PushSender;
import dev.sahilbasumatary.notificationservice.push.LoggingPushSender;
import dev.sahilbasumatary.notificationservice.push.PushSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PushProperties.class)
public class PushConfig {

    private static final Logger log = LoggerFactory.getLogger(PushConfig.class);

    @Bean
    PushSender pushSender(PushProperties properties, ObjectMapper objectMapper) {
        String provider = properties.provider() == null ? "logging" : properties.provider().trim();
        boolean fcmReady =
                "fcm".equalsIgnoreCase(provider)
                        && properties.fcmProjectId() != null
                        && !properties.fcmProjectId().isBlank()
                        && properties.fcmAccessToken() != null
                        && !properties.fcmAccessToken().isBlank();
        if (fcmReady) {
            log.info("Push provider=fcm project={}", properties.fcmProjectId());
            return new FcmHttpV1PushSender(
                    properties.fcmProjectId(), properties.fcmAccessToken(), objectMapper);
        }
        if ("fcm".equalsIgnoreCase(provider)) {
            log.warn("FCM project/token missing — falling back to logging push provider");
        } else {
            log.info("Push provider=logging");
        }
        return new LoggingPushSender();
    }
}
