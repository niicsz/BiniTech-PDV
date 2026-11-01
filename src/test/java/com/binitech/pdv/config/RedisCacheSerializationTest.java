package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.binitech.pdv.domain.Product;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

class RedisCacheSerializationTest {

  private final GenericJackson2JsonRedisSerializer serializer = RedisCacheConfig.cacheSerializer();

  private Product product() {
    Product product = new Product();
    product.setId("p1");
    product.setBarcode("7894900011517");
    product.setDescription("Refrigerante");
    product.setPrice(5.5);
    product.setTenantId("t1");
    product.setNcm("22021000");
    return product;
  }

  @Test
  @DisplayName("Lista mutável de produtos volta do cache com o tipo correto")
  void arrayListRoundTrip() {
    List<Product> products = new ArrayList<>(List.of(product()));

    Object restored = serializer.deserialize(serializer.serialize(products));

    assertTrue(restored instanceof List<?>);
    List<?> list = (List<?>) restored;
    assertEquals(1, list.size());
    assertTrue(list.get(0) instanceof Product);
    assertEquals("22021000", ((Product) list.get(0)).getNcm());
  }

  @Test
  @DisplayName("Lista vazia também volta do cache")
  void emptyArrayListRoundTrip() {
    Object restored = serializer.deserialize(serializer.serialize(new ArrayList<Product>()));

    assertEquals(List.of(), restored);
  }

  @Test
  @DisplayName("Stream.toList() (classe final) não guarda o tipo e quebra a leitura")
  void immutableListCannotBeRead() {
    byte[] cached = serializer.serialize(List.of(product()).stream().toList());

    assertThrows(SerializationException.class, () -> serializer.deserialize(cached));
  }
}
