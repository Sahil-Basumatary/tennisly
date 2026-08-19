package dev.sahilbasumatary.analyticsservice.config;

import dev.sahilbasumatary.common.security.InternalToken;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AnalyticsClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient matchServiceRestClient(
            @LoadBalanced RestClient.Builder loadBalancedBuilder,
            AnalyticsClientProperties properties,
            @Value("${tennisly.internal-token:}") String internalToken) {
        String uri = properties.matchServiceUri();
        RestClient.Builder builder =
                usesDiscovery(uri) ? loadBalancedBuilder.clone() : RestClient.builder();
        builder.baseUrl(uri).requestFactory(requestFactory(properties));
        if (InternalToken.isEnabled(internalToken)) {
            builder.defaultHeader(InternalToken.HEADER, internalToken);
        }
        return builder.build();
    }

    private static boolean usesDiscovery(String uri) {
        String host = URI.create(uri).getHost();
        // Eureka ids have no dots; Render hostnames do. Port-only checks treat HTTPS as discovery.
        return host != null && !host.contains(".");
    }

    private ClientHttpRequestFactory requestFactory(AnalyticsClientProperties properties) {
        ClientHttpRequestFactorySettings settings =
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(properties.connectTimeout())
                        .withReadTimeout(properties.readTimeout());
        return ClientHttpRequestFactories.get(settings);
    }
}
