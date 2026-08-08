package dev.sahilbasumatary.analyticsservice.config;

import java.net.URI;
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
            AnalyticsClientProperties properties) {
        String uri = properties.matchServiceUri();
        RestClient.Builder builder =
                usesDiscovery(uri) ? loadBalancedBuilder.clone() : RestClient.builder();
        return builder.baseUrl(uri).requestFactory(requestFactory(properties)).build();
    }

    /**
     * The load balancer resolves the host as a Eureka service id, so an absolute host:port
     * override from local runs must bypass it rather than be looked up as "localhost".
     */
    private static boolean usesDiscovery(String uri) {
        return URI.create(uri).getPort() == -1;
    }

    private ClientHttpRequestFactory requestFactory(AnalyticsClientProperties properties) {
        ClientHttpRequestFactorySettings settings =
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(properties.connectTimeout())
                        .withReadTimeout(properties.readTimeout());
        return ClientHttpRequestFactories.get(settings);
    }
}
