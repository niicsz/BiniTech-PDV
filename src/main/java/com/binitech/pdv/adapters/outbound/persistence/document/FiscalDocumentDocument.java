package com.binitech.pdv.adapters.outbound.persistence.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fiscal_documents")
public class FiscalDocumentDocument {

  @Id private String id;
  @Indexed private String tenantId;
  @Indexed private String saleId;
  private String providerInvoiceId;
  private String status;
  private Integer numero;
  private Integer serie;
  private String chaveAcesso;
  private String protocolo;
  private Integer codigoStatus;
  private String motivo;
  private String qrCode;
  private String urlChave;
  private BigDecimal valorTotal;
  private String consumerCpf;
  private int environment;
  private LocalDateTime issuedAt;
  private LocalDateTime cancelledAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
