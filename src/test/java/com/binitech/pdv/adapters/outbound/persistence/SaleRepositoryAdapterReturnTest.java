package com.binitech.pdv.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.adapters.outbound.persistence.document.SaleDocument;
import com.binitech.pdv.adapters.outbound.persistence.mapper.PersistenceMapperImpl;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataSaleRepository;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.SaleReturnRequest;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.RefundMethod;
import com.mongodb.client.result.UpdateResult;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class SaleRepositoryAdapterReturnTest {

  private final MongoTemplate mongo = mock(MongoTemplate.class);
  private final SaleRepositoryAdapter adapter =
      new SaleRepositoryAdapter(
          mock(SpringDataSaleRepository.class), new PersistenceMapperImpl(), mongo);

  private Sale saleWithReturn() {
    Sale sale = new Sale();
    sale.setId("s1");
    sale.setItems(new ArrayList<>(List.of(new SaleItem("p1", "Arroz", 2, 10.0, 6.0))));
    sale.recalculateTotal();
    sale.setPayments(new ArrayList<>(List.of(new Payment(PaymentMethod.PIX, 20.0))));
    sale.registerReturn(
        new SaleReturnRequest(Map.of("p1", 2), RefundMethod.PIX, "Troca", true),
        "r1",
        "u1",
        LocalDateTime.of(2026, 11, 1, 9, 0));
    return sale;
  }

  @SuppressWarnings("unchecked")
  private Document versionCondition(Query query) {
    List<Document> and = (List<Document>) query.getQueryObject().get("$and");
    return (Document) and.get(0).get("returnsVersion");
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("Primeira devolução aceita venda sem versão gravada e grava tudo de uma vez")
  void firstReturn() {
    when(mongo.updateFirst(any(Query.class), any(Update.class), eq(SaleDocument.class)))
        .thenReturn(UpdateResult.acknowledged(1, 1L, null));

    boolean saved = adapter.appendReturn(saleWithReturn(), 0);

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
    verify(mongo).updateFirst(query.capture(), update.capture(), eq(SaleDocument.class));
    assertThat(saved).isTrue();
    assertThat(query.getValue().getQueryObject().get("_id")).isEqualTo("s1");
    assertThat((List<Object>) versionCondition(query.getValue()).get("$in"))
        .containsExactly(null, 0);
    Document set = (Document) update.getValue().getUpdateObject().get("$set");
    assertThat(set.get("status")).isEqualTo("RETURNED");
    assertThat(set.get("returnedAmount")).isEqualTo(20.0);
    assertThat(set.get("returnedCost")).isEqualTo(12.0);
    assertThat(set.get("returnsVersion")).isEqualTo(1);
    assertThat((List<?>) set.get("returns")).hasSize(1);
  }

  @Test
  @DisplayName("Se outra devolução gravou antes, nada é alterado e o adapter avisa")
  void conflict() {
    when(mongo.updateFirst(any(Query.class), any(Update.class), eq(SaleDocument.class)))
        .thenReturn(UpdateResult.acknowledged(0, 0L, null));

    boolean saved = adapter.appendReturn(saleWithReturn(), 3);

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongo).updateFirst(query.capture(), any(Update.class), eq(SaleDocument.class));
    assertThat(saved).isFalse();
    assertThat(query.getValue().getQueryObject().toJson()).contains("\"returnsVersion\": 3");
  }
}
