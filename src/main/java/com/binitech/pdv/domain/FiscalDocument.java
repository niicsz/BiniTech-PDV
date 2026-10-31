package com.binitech.pdv.domain;

import com.binitech.pdv.utils.enums.FiscalDocumentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class FiscalDocument {

  private String id;
  private String tenantId;
  private String saleId;
  private String providerInvoiceId;
  private FiscalDocumentStatus status;
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

  public FiscalDocument() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getSaleId() {
    return saleId;
  }

  public void setSaleId(String saleId) {
    this.saleId = saleId;
  }

  public String getProviderInvoiceId() {
    return providerInvoiceId;
  }

  public void setProviderInvoiceId(String providerInvoiceId) {
    this.providerInvoiceId = providerInvoiceId;
  }

  public FiscalDocumentStatus getStatus() {
    return status;
  }

  public void setStatus(FiscalDocumentStatus status) {
    this.status = status;
  }

  public Integer getNumero() {
    return numero;
  }

  public void setNumero(Integer numero) {
    this.numero = numero;
  }

  public Integer getSerie() {
    return serie;
  }

  public void setSerie(Integer serie) {
    this.serie = serie;
  }

  public String getChaveAcesso() {
    return chaveAcesso;
  }

  public void setChaveAcesso(String chaveAcesso) {
    this.chaveAcesso = chaveAcesso;
  }

  public String getProtocolo() {
    return protocolo;
  }

  public void setProtocolo(String protocolo) {
    this.protocolo = protocolo;
  }

  public Integer getCodigoStatus() {
    return codigoStatus;
  }

  public void setCodigoStatus(Integer codigoStatus) {
    this.codigoStatus = codigoStatus;
  }

  public String getMotivo() {
    return motivo;
  }

  public void setMotivo(String motivo) {
    this.motivo = motivo;
  }

  public String getQrCode() {
    return qrCode;
  }

  public void setQrCode(String qrCode) {
    this.qrCode = qrCode;
  }

  public String getUrlChave() {
    return urlChave;
  }

  public void setUrlChave(String urlChave) {
    this.urlChave = urlChave;
  }

  public BigDecimal getValorTotal() {
    return valorTotal;
  }

  public void setValorTotal(BigDecimal valorTotal) {
    this.valorTotal = valorTotal;
  }

  public String getConsumerCpf() {
    return consumerCpf;
  }

  public void setConsumerCpf(String consumerCpf) {
    this.consumerCpf = consumerCpf;
  }

  public int getEnvironment() {
    return environment;
  }

  public void setEnvironment(int environment) {
    this.environment = environment;
  }

  public LocalDateTime getIssuedAt() {
    return issuedAt;
  }

  public void setIssuedAt(LocalDateTime issuedAt) {
    this.issuedAt = issuedAt;
  }

  public LocalDateTime getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(LocalDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FiscalDocument that = (FiscalDocument) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
