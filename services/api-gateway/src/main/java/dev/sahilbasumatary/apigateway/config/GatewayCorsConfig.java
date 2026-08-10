package dev.sahilbasumatary.apigateway.config;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class GatewayCorsConfig {

    @Bean
    CorsWebFilter corsWebFilter(CorsProperties properties) {
        List<String> origins = properties.resolvedOrigins();
        if (origins.isEmpty()) {
            throw new IllegalStateException(
                    "tennisly.cors.allowed-origins must contain at least one origin"
                            + " (set CORS_ALLOWED_ORIGINS)");
        }
        CorsConfiguration config = new CorsConfiguration();
        origins.forEach(config::addAllowedOrigin);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
