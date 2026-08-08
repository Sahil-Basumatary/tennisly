package dev.sahilbasumatary.analyticsservice.config;

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
            @LoadBalanced RestClient.Builder builder, AnalyticsClientProperties properties) {
        return builder.clone()
                .baseUrl(properties.matchServiceUri())
                .requestFactory(requestFactory(properties))
                .build();
    }

    private ClientHttpRequestFactory requestFactory(AnalyticsClientProperties properties) {
        ClientHttpRequestFactorySettings settings =
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(properties.connectTimeout())
                        .withReadTimeout(properties.readTimeout());
        return ClientHttpRequestFactories.get(settings);
    }
}
