package com.binitech.pdv.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.binitech.pdv.adapters.outbound.persistence.document.DeliveryStoreDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.PixSettingsDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.PreSaleCounterDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.PreSaleDocument;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

class TenantPurgeQueryMappingTest {

  private final MongoMappingContext context = mappingContext();
  private final QueryMapper mapper =
      new QueryMapper(new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context));

  private static MongoMappingContext mappingContext() {
    MongoMappingContext context = new MongoMappingContext();
    context.setSimpleTypeHolder(new MongoCustomConversions(List.of()).getSimpleTypeHolder());
    return context;
  }

  private Document mapped(Class<?> type) {
    return mapper.getMappedObject(
        new Document("tenantId", "t1"), context.getRequiredPersistentEntity(type));
  }

  @ParameterizedTest
  @ValueSource(
      classes = {
        PreSaleCounterDocument.class,
        DeliveryStoreDocument.class,
        PixSettingsDocument.class
      })
  @DisplayName("Na limpeza da loja, documentos com o id igual ao da loja são achados pelo _id")
  void tenantKeyedDocumentsMatchById(Class<?> type) {
    assertThat(mapped(type)).isEqualTo(new Document("_id", "t1"));
  }

  @ParameterizedTest
  @ValueSource(classes = {PreSaleDocument.class})
  @DisplayName("Na limpeza da loja, documentos comuns são achados pelo campo tenantId")
  void regularDocumentsMatchByTenantField(Class<?> type) {
    assertThat(mapped(type)).isEqualTo(new Document("tenantId", "t1"));
  }
}
