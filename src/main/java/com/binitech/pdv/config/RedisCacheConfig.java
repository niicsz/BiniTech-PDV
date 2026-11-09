package com.binitech.pdv.config;

import java.time.Duration;
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
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

  private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

  private static final String CACHE_VERSION = "v4";

  static GenericJacksonJsonRedisSerializer cacheSerializer() {
    JsonMapper cacheMapper =
        JsonMapper.builder()
            .activateDefaultTypingAsProperty(
                BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType("com.binitech.pdv.")
                    .allowIfSubType("java.util.")
                    .allowIfSubType("java.math.")
                    .allowIfSubType("java.time.")
                    .build(),
                DefaultTyping.NON_FINAL,
                "@class")
            .build();
    return new GenericJacksonJsonRedisSerializer(cacheMapper);
  }

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    GenericJacksonJsonRedisSerializer serializer = cacheSerializer();

    RedisCacheConfiguration defaultConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .computePrefixWith(cacheName -> "pdv:" + CACHE_VERSION + ":" + cacheName + "::")
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .disableCachingNullValues();

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withCacheConfiguration("products_by_user", defaultConfig.entryTtl(Duration.ofMinutes(5)))
        .withCacheConfiguration("product_by_id", defaultConfig.entryTtl(Duration.ofMinutes(10)))
        .withCacheConfiguration(
            "product_by_barcode", defaultConfig.entryTtl(Duration.ofMinutes(10)))
        .withCacheConfiguration("products_all", defaultConfig.entryTtl(Duration.ofMinutes(5)))
        .build();
  }

  @Override
  public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {

      @Override
      public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
            "Cache GET error – tratando como cache miss. cache='{}' key='{}': {}",
            cache.getName(),
            key,
            exception.getMessage());
      }

      @Override
      public void handleCachePutError(
          RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn(
            "Cache PUT error. cache='{}' key='{}': {}",
            cache.getName(),
            key,
            exception.getMessage());
      }

      @Override
      public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
            "Cache EVICT error. cache='{}' key='{}': {}",
            cache.getName(),
            key,
            exception.getMessage());
      }

      @Override
      public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache CLEAR error. cache='{}': {}", cache.getName(), exception.getMessage());
      }
    };
  }
}
