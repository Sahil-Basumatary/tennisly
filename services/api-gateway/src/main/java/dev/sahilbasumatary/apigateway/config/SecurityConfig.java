package dev.sahilbasumatary.apigateway.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${CLERK_ISSUER_URI:}")
    private String clerkIssuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .headers(
                        headers ->
                                headers
                                        .contentTypeOptions(Customizer.withDefaults())
                                        .frameOptions(
                                                frame ->
                                                        frame.mode(
                                                                XFrameOptionsServerHttpHeadersWriter
                                                                        .Mode.DENY))
                                        .hsts(
                                                hsts ->
                                                        hsts.includeSubdomains(true)
                                                                .maxAge(Duration.ofDays(365)))
                                        .referrerPolicy(
                                                referrer ->
                                                        referrer.policy(
                                                                ReferrerPolicyServerHttpHeadersWriter
                                                                        .ReferrerPolicy
                                                                        .NO_REFERRER))
                                        .permissionsPolicy(
                                                permissions ->
                                                        permissions.policy(
                                                                "camera=(), microphone=(),"
                                                                        + " geolocation=()")))
                .authorizeExchange(exchanges -> exchanges
                        // CORS preflight must pass without a token
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .pathMatchers("/actuator/**").denyAll()
                        .pathMatchers(
                                "/health",
                                "/info",
                                "/api/auth/webhooks/**"
                        ).permitAll()
                        .pathMatchers("/api/v1/**").permitAll()
                        // Phase 8a: Vercel BFF hits these without a Clerk JWT. Mutations and
                        // sync stay authenticated so the BallDontLie quota cannot be drained
                        // through the public gateway.
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/tennis/players/**",
                                "/api/tennis/rankings/**",
                                "/api/tennis/tournaments/**",
                                "/api/tennis/shot-distributions/**",
                                "/api/matches/**")
                        .permitAll()
                        .pathMatchers("/ws/matches/**").permitAll()
                        .pathMatchers("/api/analytics/views/**").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/analytics/**").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder()))
                )
                .build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        NimbusReactiveJwtDecoder decoder =
                NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> issuerValidator =
                clerkIssuerUri != null && !clerkIssuerUri.isBlank()
                        ? new JwtClaimValidator<>("iss", clerkIssuerUri::equals)
                        : jwt -> org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(defaultValidator, issuerValidator));
        return decoder;
    }
}
