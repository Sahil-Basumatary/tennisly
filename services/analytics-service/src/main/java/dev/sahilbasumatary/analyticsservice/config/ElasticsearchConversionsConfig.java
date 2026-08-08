package dev.sahilbasumatary.analyticsservice.config;

import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;

@Configuration
public class ElasticsearchConversionsConfig {

    @Bean
    ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(
                List.of(new LongToInstantConverter(), new InstantToLongConverter()));
    }

    @ReadingConverter
    static final class LongToInstantConverter implements Converter<Long, Instant> {
        @Override
        public Instant convert(Long source) {
            return Instant.ofEpochMilli(source);
        }
    }

    @WritingConverter
    static final class InstantToLongConverter implements Converter<Instant, Long> {
        @Override
        public Long convert(Instant source) {
            return source.toEpochMilli();
        }
    }
}
