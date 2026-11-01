package com.binitech.pdv.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.adapters.outbound.persistence.document.ProductDocument;
import com.binitech.pdv.adapters.outbound.persistence.mapper.PersistenceMapperImpl;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataProductRepository;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate;
import org.springframework.data.mongodb.core.aggregation.TypeBasedAggregationOperationContext;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class ProductRepositoryAdapterStockTest {

  private final MongoTemplate mongo = mock(MongoTemplate.class);
  private final ProductRepositoryAdapter adapter =
      new ProductRepositoryAdapter(
          mock(SpringDataProductRepository.class), new PersistenceMapperImpl(), mongo);

  @Test
  @DisplayName("Baixa atômica: só decrementa se houver estoque, com $inc negativo")
  void decreaseIsConditionalAndAtomic() {
    when(mongo.updateFirst(any(Query.class), any(Update.class), eq(ProductDocument.class)))
        .thenReturn(UpdateResult.acknowledged(1, 1L, null));

    assertThat(adapter.decreaseStock("p1", "t1", 3)).isTrue();

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
    verify(mongo).updateFirst(query.capture(), update.capture(), eq(ProductDocument.class));
    Document filter = query.getValue().getQueryObject();
    assertThat(filter).containsEntry("_id", "p1").containsEntry("tenantId", "t1");
    assertThat((Document) filter.get("stockQuantity")).containsEntry("$gte", 3);
    assertThat((Document) update.getValue().getUpdateObject().get("$inc"))
        .containsEntry("stockQuantity", -3);
  }

  @Test
  @DisplayName("Sem estoque suficiente nada muda e o adapter avisa")
  void decreaseFailsWithoutStock() {
    when(mongo.updateFirst(any(Query.class), any(Update.class), eq(ProductDocument.class)))
        .thenReturn(UpdateResult.acknowledged(0, 0L, null));

    assertThat(adapter.decreaseStock("p1", "t1", 3)).isFalse();
    assertThat(adapter.increaseStock("p1", "t1", 3)).isFalse();
  }

  @Test
  @DisplayName("Reposição usa $inc positivo filtrando pela loja")
  void increaseIsAtomic() {
    when(mongo.updateFirst(any(Query.class), any(Update.class), eq(ProductDocument.class)))
        .thenReturn(UpdateResult.acknowledged(1, 1L, null));

    assertThat(adapter.increaseStock("p1", "t1", 2)).isTrue();

    ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
    verify(mongo).updateFirst(any(Query.class), update.capture(), eq(ProductDocument.class));
    assertThat((Document) update.getValue().getUpdateObject().get("$inc"))
        .containsEntry("stockQuantity", 2);
  }

  @Test
  @DisplayName("Venda sem validar estoque baixa só o que existe, nunca abaixo de zero")
  void decreaseUpToFloorsAtZero() {
    ProductDocument before = new ProductDocument();
    before.setStockQuantity(2);
    when(mongo.findAndModify(
            any(Query.class),
            any(AggregationUpdate.class),
            any(FindAndModifyOptions.class),
            eq(ProductDocument.class)))
        .thenReturn(before);

    assertThat(adapter.decreaseStockUpTo("p1", "t1", 5)).isEqualTo(2);

    ArgumentCaptor<AggregationUpdate> update = ArgumentCaptor.forClass(AggregationUpdate.class);
    verify(mongo)
        .findAndModify(
            any(Query.class),
            update.capture(),
            any(FindAndModifyOptions.class),
            eq(ProductDocument.class));
    MongoMappingContext context = new MongoMappingContext();
    List<Document> pipeline =
        update
            .getValue()
            .toPipeline(
                new TypeBasedAggregationOperationContext(
                    ProductDocument.class, context, new QueryMapper(mongoConverter(context))));
    assertThat(pipeline.get(0).toJson())
        .contains("\"$max\": [0, {\"$subtract\": [\"$stockQuantity\", 5]}]");

    when(mongo.findAndModify(
            any(Query.class),
            any(AggregationUpdate.class),
            any(FindAndModifyOptions.class),
            eq(ProductDocument.class)))
        .thenReturn(null);
    assertThat(adapter.decreaseStockUpTo("p9", "t1", 5)).isZero();
  }

  private static org.springframework.data.mongodb.core.convert.MappingMongoConverter mongoConverter(
      MongoMappingContext context) {
    return new org.springframework.data.mongodb.core.convert.MappingMongoConverter(
        org.springframework.data.mongodb.core.convert.NoOpDbRefResolver.INSTANCE, context);
  }
}
