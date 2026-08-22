package dev.sahilbasumatary.tennisdataservice.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.sahilbasumatary.tennisdataservice.dto.response.PlayerResponse;
import dev.sahilbasumatary.tennisdataservice.dto.response.RankingResponse;
import dev.sahilbasumatary.tennisdataservice.dto.response.ShotDistributionResponse;
import dev.sahilbasumatary.tennisdataservice.dto.response.TournamentResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    public static final String PLAYER_CACHE = "player";
    public static final String PLAYERS_CACHE = "players";
    public static final String PLAYER_RANKINGS_CACHE = "playerRankings";
    public static final String TOURNAMENTS_CACHE = "tournaments";
    public static final String RANKINGS_CACHE = "rankings";
    public static final String SHOT_DISTRIBUTIONS_CACHE = "shotDistributions";

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = buildObjectMapper();
        RedisCacheConfiguration base =
                RedisCacheConfiguration.defaultCacheConfig()
                        .disableCachingNullValues()
                        .computePrefixWith(name -> "tennis:" + name + ":")
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        new StringRedisSerializer()));

        Map<String, RedisCacheConfiguration> caches = new HashMap<>();
        caches.put(
                PLAYER_CACHE,
                typedCache(base, objectMapper, Duration.ofHours(1), PlayerResponse.class));
        caches.put(
                PLAYERS_CACHE,
                listCache(base, objectMapper, Duration.ofHours(1), PlayerResponse.class));
        caches.put(
                PLAYER_RANKINGS_CACHE,
                listCache(base, objectMapper, Duration.ofHours(6), RankingResponse.class));
        caches.put(
                TOURNAMENTS_CACHE,
                listCache(base, objectMapper, Duration.ofHours(24), TournamentResponse.class));
        caches.put(
                RANKINGS_CACHE,
                listCache(base, objectMapper, Duration.ofHours(6), RankingResponse.class));
        caches.put(
                SHOT_DISTRIBUTIONS_CACHE,
                listCache(
                        base,
                        objectMapper,
                        Duration.ofHours(24),
                        ShotDistributionResponse.class));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(Duration.ofHours(1)))
                .withInitialCacheConfigurations(caches)
                .enableStatistics()
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(
                    @NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.warn("Redis cache get failed cache={} key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(
                    @NonNull RuntimeException exception,
                    @NonNull Cache cache,
                    @NonNull Object key,
                    @Nullable Object value) {
                log.warn("Redis cache put failed cache={} key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(
                    @NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.warn("Redis cache evict failed cache={} key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(
                    @NonNull RuntimeException exception, @NonNull Cache cache) {
                log.warn("Redis cache clear failed cache={}", cache.getName(), exception);
            }
        };
    }

    private <T> RedisCacheConfiguration typedCache(
            RedisCacheConfiguration base,
            ObjectMapper objectMapper,
            Duration ttl,
            Class<T> valueType) {
        JavaType javaType = objectMapper.getTypeFactory().constructType(valueType);
        return cacheWithSerializer(base, objectMapper, ttl, javaType);
    }

    private <T> RedisCacheConfiguration listCache(
            RedisCacheConfiguration base,
            ObjectMapper objectMapper,
            Duration ttl,
            Class<T> elementType) {
        JavaType javaType =
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return cacheWithSerializer(base, objectMapper, ttl, javaType);
    }

    private RedisCacheConfiguration cacheWithSerializer(
            RedisCacheConfiguration base,
            ObjectMapper objectMapper,
            Duration ttl,
            JavaType javaType) {
        return base.entryTtl(ttl)
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new TypedJsonRedisSerializer(objectMapper, javaType)));
    }

    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private static final class TypedJsonRedisSerializer implements RedisSerializer<Object> {

        private final ObjectMapper objectMapper;
        private final JavaType javaType;

        private TypedJsonRedisSerializer(ObjectMapper objectMapper, JavaType javaType) {
            this.objectMapper = objectMapper;
            this.javaType = javaType;
        }

        @Override
        public byte[] serialize(@Nullable Object value) throws SerializationException {
            if (value == null) {
                return new byte[0];
            }
            try {
                return objectMapper.writerFor(javaType).writeValueAsBytes(value);
            } catch (JsonProcessingException ex) {
                throw new SerializationException("Could not serialize Redis cache value", ex);
            }
        }

        @Override
        @Nullable
        public Object deserialize(@Nullable byte[] bytes) throws SerializationException {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            try {
                return objectMapper.readerFor(javaType).readValue(bytes);
            } catch (IOException ex) {
                throw new SerializationException("Could not deserialize Redis cache value", ex);
            }
        }
    }
}
