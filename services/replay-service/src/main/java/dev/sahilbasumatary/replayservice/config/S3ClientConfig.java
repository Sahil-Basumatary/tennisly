package dev.sahilbasumatary.replayservice.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Builds the S3 client. When an explicit endpoint is configured (MinIO in dev) it is overridden with
 * static credentials and path-style access; otherwise the default AWS provider chain is used so
 * production can rely on IAM roles rather than baked-in secrets.
 */
@Configuration
public class S3ClientConfig {

    @Bean
    S3Client s3Client(ReplayStorageProperties properties) {
        S3ClientBuilder builder =
                S3Client.builder()
                        .region(Region.of(properties.region()))
                        .serviceConfiguration(
                                S3Configuration.builder()
                                        .pathStyleAccessEnabled(properties.pathStyleAccess())
                                        .build());
        if (StringUtils.hasText(properties.endpoint())) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
        if (StringUtils.hasText(properties.accessKey())
                && StringUtils.hasText(properties.secretKey())) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    properties.accessKey(), properties.secretKey())));
        }
        return builder.build();
    }
}
