package com.binitech.pdv.application.ports.outbound;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FiscalEmitterPort {

  boolean isConfigured();

  int environment();

  String createIssuer(IssuerData issuer);

  void updateIssuer(String issuerId, IssuerData issuer);

  LocalDate uploadCertificate(String issuerId, byte[] certificate, String password);

  String createIssuerApiKey(String issuerId);

  String issueNfce(String apiKey, NfceRequest request, String idempotencyKey);

  NfceStatus fetchStatus(String apiKey, String invoiceId);

  void cancel(String apiKey, String invoiceId, String reason);

  byte[] downloadDanfe(String apiKey, String invoiceId);

  record IssuerData(
      String name,
      String cnpj,
      String razaoSocial,
      String inscricaoEstadual,
      String regimeTributario,
      String codigoMunicipio,
      String email,
      String telefone,
      Address endereco,
      int serieNfce,
      String cscId,
      String cscToken) {}

  record Address(
      String logradouro,
      String numero,
      String complemento,
      String bairro,
      String cidade,
      String uf,
      String cep) {}

  record NfceRequest(Consumer consumer, List<NfceItem> items, List<NfcePayment> payments) {}

  record Consumer(String cpf, String nome) {}

  record NfceItem(
      String codigo,
      String descricao,
      String ncm,
      String cfop,
      String csosn,
      String ean,
      BigDecimal quantidade,
      BigDecimal valorUnitario,
      BigDecimal valorTotal) {}

  record NfcePayment(String tipoPagamento, BigDecimal valor) {}

  record NfceStatus(
      String status,
      Integer numero,
      Integer serie,
      String chaveAcesso,
      String protocolo,
      Integer codigoStatus,
      String motivo,
      String qrCode,
      String urlChave,
      BigDecimal valorTotal,
      String errorMessage) {}
}
