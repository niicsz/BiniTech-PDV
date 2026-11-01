package com.binitech.pdv.config;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

public final class RedisCacheTestSupport {

  private RedisCacheTestSupport() {}

  public static Object roundTrip(Object value) {
    GenericJackson2JsonRedisSerializer serializer = RedisCacheConfig.cacheSerializer();
    return serializer.deserialize(serializer.serialize(value));
  }
}
