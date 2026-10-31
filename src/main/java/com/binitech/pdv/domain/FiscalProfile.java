package com.binitech.pdv.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class FiscalProfile {

  private String id;
  private String tenantId;
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

  public FiscalProfile() {}

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

  public String getCnpj() {
    return cnpj;
  }

  public void setCnpj(String cnpj) {
    this.cnpj = cnpj;
  }

  public String getRazaoSocial() {
    return razaoSocial;
  }

  public void setRazaoSocial(String razaoSocial) {
    this.razaoSocial = razaoSocial;
  }

  public String getInscricaoEstadual() {
    return inscricaoEstadual;
  }

  public void setInscricaoEstadual(String inscricaoEstadual) {
    this.inscricaoEstadual = inscricaoEstadual;
  }

  public String getRegimeTributario() {
    return regimeTributario;
  }

  public void setRegimeTributario(String regimeTributario) {
    this.regimeTributario = regimeTributario;
  }

  public String getCodigoMunicipio() {
    return codigoMunicipio;
  }

  public void setCodigoMunicipio(String codigoMunicipio) {
    this.codigoMunicipio = codigoMunicipio;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getTelefone() {
    return telefone;
  }

  public void setTelefone(String telefone) {
    this.telefone = telefone;
  }

  public String getLogradouro() {
    return logradouro;
  }

  public void setLogradouro(String logradouro) {
    this.logradouro = logradouro;
  }

  public String getNumero() {
    return numero;
  }

  public void setNumero(String numero) {
    this.numero = numero;
  }

  public String getComplemento() {
    return complemento;
  }

  public void setComplemento(String complemento) {
    this.complemento = complemento;
  }

  public String getBairro() {
    return bairro;
  }

  public void setBairro(String bairro) {
    this.bairro = bairro;
  }

  public String getCidade() {
    return cidade;
  }

  public void setCidade(String cidade) {
    this.cidade = cidade;
  }

  public String getUf() {
    return uf;
  }

  public void setUf(String uf) {
    this.uf = uf;
  }

  public String getCep() {
    return cep;
  }

  public void setCep(String cep) {
    this.cep = cep;
  }

  public int getSerieNfce() {
    return serieNfce;
  }

  public void setSerieNfce(int serieNfce) {
    this.serieNfce = serieNfce;
  }

  public String getCscId() {
    return cscId;
  }

  public void setCscId(String cscId) {
    this.cscId = cscId;
  }

  public boolean isCscConfigured() {
    return cscConfigured;
  }

  public void setCscConfigured(boolean cscConfigured) {
    this.cscConfigured = cscConfigured;
  }

  public String getDefaultNcm() {
    return defaultNcm;
  }

  public void setDefaultNcm(String defaultNcm) {
    this.defaultNcm = defaultNcm;
  }

  public String getDefaultCfop() {
    return defaultCfop;
  }

  public void setDefaultCfop(String defaultCfop) {
    this.defaultCfop = defaultCfop;
  }

  public String getDefaultCsosn() {
    return defaultCsosn;
  }

  public void setDefaultCsosn(String defaultCsosn) {
    this.defaultCsosn = defaultCsosn;
  }

  public boolean isAutoIssue() {
    return autoIssue;
  }

  public void setAutoIssue(boolean autoIssue) {
    this.autoIssue = autoIssue;
  }

  public String getProviderProjectId() {
    return providerProjectId;
  }

  public void setProviderProjectId(String providerProjectId) {
    this.providerProjectId = providerProjectId;
  }

  public String getProviderApiKeyEncrypted() {
    return providerApiKeyEncrypted;
  }

  public void setProviderApiKeyEncrypted(String providerApiKeyEncrypted) {
    this.providerApiKeyEncrypted = providerApiKeyEncrypted;
  }

  public LocalDate getCertificateValidUntil() {
    return certificateValidUntil;
  }

  public void setCertificateValidUntil(LocalDate certificateValidUntil) {
    this.certificateValidUntil = certificateValidUntil;
  }

  public int getEnvironment() {
    return environment;
  }

  public void setEnvironment(int environment) {
    this.environment = environment;
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
    FiscalProfile that = (FiscalProfile) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
