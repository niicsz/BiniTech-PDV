package com.binitech.pdv.adapters.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.binitech.pdv.adapters.outbound.persistence.document.ProductDocument;
import com.binitech.pdv.adapters.outbound.persistence.mapper.PersistenceMapper;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataProductRepository;
import com.binitech.pdv.config.RedisCacheTestSupport;
import com.binitech.pdv.domain.Product;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class ProductRepositoryAdapterCacheTest {

  @Test
  @DisplayName("Listas de produtos em cache voltam do Redis (produtos da loja, do usuário e todos)")
  void cachedListsSurviveRedisRoundTrip() {
    SpringDataProductRepository repository = mock(SpringDataProductRepository.class);
    PersistenceMapper mapper = mock(PersistenceMapper.class);
    ProductDocument document = new ProductDocument();
    Product product = new Product();
    product.setId("p1");
    product.setDescription("Refrigerante");
    when(mapper.toDomain(any(ProductDocument.class))).thenReturn(product);
    when(repository.findAllByTenantId(eq("t1"), any(Pageable.class))).thenReturn(List.of(document));
    when(repository.findAllByUserId(eq("u1"), any(Pageable.class))).thenReturn(List.of(document));
    when(repository.findAll(any(Pageable.class)))
        .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(document)));
    ProductRepositoryAdapter adapter = new ProductRepositoryAdapter(repository, mapper, null);

    for (List<Product> cached :
        List.of(
            adapter.findAllByTenantId("t1", 0, 200),
            adapter.findAllByUserId("u1", 0, 200),
            adapter.findAll(0, 200))) {
      Object restored = RedisCacheTestSupport.roundTrip(cached);
      assertEquals(1, ((List<?>) restored).size());
      assertEquals("p1", ((Product) ((List<?>) restored).get(0)).getId());
    }
  }
}
