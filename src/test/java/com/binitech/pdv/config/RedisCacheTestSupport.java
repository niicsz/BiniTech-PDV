package com.binitech.pdv.config;

import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

public final class RedisCacheTestSupport {

  private RedisCacheTestSupport() {}

  public static Object roundTrip(Object value) {
    GenericJacksonJsonRedisSerializer serializer = RedisCacheConfig.cacheSerializer();
    return serializer.deserialize(serializer.serialize(value));
  }
}
