package com.binitech.pdv.adapters.outbound.persistence.document;

import java.time.LocalDate;
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
@Document(collection = "fiscal_profiles")
public class FiscalProfileDocument {

  @Id private String id;
  @Indexed private String tenantId;
  private String cnpj;
  private String razaoSocial;
  private String inscricaoEstadual;
  private String regimeTributario;
  private String codigoMunicipio;
  private String email;
  private String telefone;
  private String logradouro;
  private String numero;
  private String complemento;
  private String bairro;
  private String cidade;
  private String uf;
  private String cep;
  private int serieNfce;
  private String cscId;
  private boolean cscConfigured;
  private String defaultNcm;
  private String defaultCfop;
  private String defaultCsosn;
  private boolean autoIssue;
  private String providerProjectId;
  private String providerApiKeyEncrypted;
  private LocalDate certificateValidUntil;
  private int environment;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
