package dev.sahilbasumatary.matchservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MatchClientProperties.class)
public class MatchClientConfig {

    @Bean
    RestClient tennisDataServiceRestClient(MatchClientProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder()
                .baseUrl(properties.getTennisDataServiceUri().replaceAll("/$", ""))
                .requestFactory(factory)
                .build();
    }
}
