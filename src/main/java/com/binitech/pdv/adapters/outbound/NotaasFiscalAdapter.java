package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.application.ports.outbound.FiscalEmitterPort;
import com.binitech.pdv.domain.exception.FiscalProviderException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NotaasFiscalAdapter implements FiscalEmitterPort {

  private static final Logger log = LoggerFactory.getLogger(NotaasFiscalAdapter.class);
  private static final String API_KEY_HEADER = "x-api-key";
  private static final int MAX_ERROR_BODY = 500;

  private final RestClient client;
  private final String orgToken;
  private final int environment;

  public NotaasFiscalAdapter(
      RestClient.Builder restClientBuilder,
      @Value("${notaas.org-token:}") String orgToken,
      @Value("${notaas.base-url:https://platform.notaas.com.br/api/v1}") String baseUrl,
      @Value("${notaas.environment:2}") int environment) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5_000);
    factory.setReadTimeout(20_000);
    this.client =
        restClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.USER_AGENT, "BiniTech-PDV/1.0")
            .requestFactory(factory)
            .build();
    this.orgToken = orgToken;
    this.environment = environment;
    if (!isConfigured()) {
      log.warn("NOTAAS_ORG_TOKEN ausente; emissão de NFC-e desabilitada.");
    }
  }

  @Override
  public boolean isConfigured() {
    return orgToken != null && !orgToken.isBlank();
  }

  @Override
  public int environment() {
    return environment;
  }

  @Override
  public String createIssuer(IssuerData issuer) {
    ProjectResponse response =
        call(
            "criar o emitente",
            () ->
                client
                    .post()
                    .uri("/org/projects")
                    .header(API_KEY_HEADER, orgToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ProjectRequest.from(issuer, environment))
                    .retrieve()
                    .body(ProjectResponse.class));
    if (response == null || response.id() == null) {
      throw new FiscalProviderException("O emissor fiscal não retornou o id do emitente.");
    }
    return response.id();
  }

  @Override
  public void updateIssuer(String issuerId, IssuerData issuer) {
    call(
        "atualizar o emitente",
        () ->
            client
                .patch()
                .uri("/org/projects/{id}", issuerId)
                .header(API_KEY_HEADER, orgToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ProjectRequest.from(issuer, environment))
                .retrieve()
                .toBodilessEntity());
  }

  @Override
  public LocalDate uploadCertificate(String issuerId, byte[] certificate, String password) {
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add(
        "file",
        new ByteArrayResource(certificate) {
          @Override
          public String getFilename() {
            return "certificado.pfx";
          }
        });
    form.add("password", password);
    CertificateResponse response =
        call(
            "enviar o certificado A1",
            () ->
                client
                    .post()
                    .uri("/org/projects/{id}/certificate", issuerId)
                    .header(API_KEY_HEADER, orgToken)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(CertificateResponse.class));
    return response == null ? null : toLocalDate(response.validUntil());
  }

  @Override
  public String createIssuerApiKey(String issuerId) {
    ApiKeyResponse response =
        call(
            "gerar a chave do emitente",
            () ->
                client
                    .post()
                    .uri("/org/projects/{id}/api-keys", issuerId)
                    .header(API_KEY_HEADER, orgToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ApiKeyRequest("BiniTech PDV"))
                    .retrieve()
                    .body(ApiKeyResponse.class));
    if (response == null || response.key() == null) {
      throw new FiscalProviderException("O emissor fiscal não retornou a chave do emitente.");
    }
    return response.key();
  }

  @Override
  public String issueNfce(String apiKey, NfceRequest request, String idempotencyKey) {
    EmitResponse response =
        call(
            "emitir a NFC-e",
            () ->
                client
                    .post()
                    .uri("/nfe/emitir")
                    .header(API_KEY_HEADER, apiKey)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(EmitRequest.from(request))
                    .retrieve()
                    .body(EmitResponse.class));
    if (response == null || response.invoiceId() == null) {
      throw new FiscalProviderException("O emissor fiscal não retornou o id da NFC-e.");
    }
    return response.invoiceId();
  }

  @Override
  public NfceStatus fetchStatus(String apiKey, String invoiceId) {
    StatusResponse response =
        call(
            "consultar a NFC-e",
            () ->
                client
                    .get()
                    .uri("/nfe/invoices/{id}/status", invoiceId)
                    .header(API_KEY_HEADER, apiKey)
                    .retrieve()
                    .body(StatusResponse.class));
    if (response == null) {
      throw new FiscalProviderException("O emissor fiscal não retornou o status da NFC-e.");
    }
    return new NfceStatus(
        response.status(),
        response.numero(),
        response.serie(),
        response.chaveAcesso(),
        response.protocolo(),
        response.codigoStatus(),
        response.motivo(),
        response.qrCode(),
        response.urlChave(),
        response.valorTotal(),
        response.errorMessage());
  }

  @Override
  public void cancel(String apiKey, String invoiceId, String reason) {
    call(
        "cancelar a NFC-e",
        () ->
            client
                .post()
                .uri("/nfe/cancelar")
                .header(API_KEY_HEADER, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CancelRequest(invoiceId, reason))
                .retrieve()
                .toBodilessEntity());
  }

  @Override
  public byte[] downloadDanfe(String apiKey, String invoiceId) {
    return call(
        "baixar o DANFE da NFC-e",
        () ->
            client
                .get()
                .uri("/nfe/invoices/{id}/danfe", invoiceId)
                .header(API_KEY_HEADER, apiKey)
                .accept(MediaType.APPLICATION_PDF)
                .retrieve()
                .body(byte[].class));
  }

  private <T> T call(String action, ProviderCall<T> providerCall) {
    try {
      return providerCall.execute();
    } catch (RestClientResponseException e) {
      String body = e.getResponseBodyAsString();
      if (log.isWarnEnabled()) {
        log.warn(
            "Notaas recusou {}: HTTP {} {}",
            action,
            e.getStatusCode().value(),
            body.length() > MAX_ERROR_BODY ? body.substring(0, MAX_ERROR_BODY) : body);
      }
      throw new FiscalProviderException(
          "O emissor fiscal recusou " + action + ": " + extractMessage(e), e);
    } catch (RestClientException e) {
      throw new FiscalProviderException("Falha de comunicação ao " + action + ".", e);
    }
  }

  private String extractMessage(RestClientResponseException e) {
    try {
      ErrorResponse error = e.getResponseBodyAs(ErrorResponse.class);
      if (error != null) {
        String message = error.message() != null ? error.message() : error.error();
        if (message != null && !message.isBlank()) {
          return message;
        }
      }
    } catch (RuntimeException ignored) {
      return "HTTP " + e.getStatusCode().value();
    }
    return "HTTP " + e.getStatusCode().value();
  }

  private LocalDate toLocalDate(String isoDateTime) {
    if (isoDateTime == null || isoDateTime.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(isoDateTime)
          .atZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
          .toLocalDate();
    } catch (DateTimeParseException e) {
      return LocalDate.parse(isoDateTime.substring(0, 10));
    }
  }

  @FunctionalInterface
  private interface ProviderCall<T> {
    T execute();
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record ProjectRequest(
      String name,
      String cnpj,
      String razaoSocial,
      String inscricaoEstadual,
      String regimeTributario,
      String codigoMunicipio,
      String email,
      String telefone,
      Address endereco,
      Integer ambiente,
      Integer serieNfce,
      String cscId,
      String cscToken) {

    static ProjectRequest from(IssuerData issuer, int environment) {
      return new ProjectRequest(
          issuer.name(),
          issuer.cnpj(),
          issuer.razaoSocial(),
          issuer.inscricaoEstadual(),
          issuer.regimeTributario(),
          issuer.codigoMunicipio(),
          issuer.email(),
          issuer.telefone(),
          issuer.endereco(),
          environment,
          issuer.serieNfce(),
          issuer.cscId(),
          issuer.cscToken());
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record EmitRequest(
      int modelo,
      String naturezaOperacao,
      int consumidorFinal,
      int presencaComprador,
      Dest dest,
      List<Item> items,
      List<Pagamento> pagamentos) {

    static EmitRequest from(NfceRequest request) {
      Dest dest =
          request.consumer() == null || request.consumer().cpf() == null
              ? null
              : new Dest(request.consumer().cpf(), request.consumer().nome());
      return new EmitRequest(
          65,
          "Venda a consumidor",
          1,
          1,
          dest,
          request.items().stream()
              .map(
                  item ->
                      new Item(
                          item.codigo(),
                          item.descricao(),
                          item.ncm(),
                          item.cfop(),
                          item.csosn(),
                          item.ean(),
                          item.quantidade(),
                          item.valorUnitario(),
                          item.valorTotal()))
              .toList(),
          request.payments().stream()
              .map(payment -> new Pagamento(payment.tipoPagamento(), payment.valor()))
              .toList());
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record Dest(String cpf, String nome) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record Item(
      String codigo,
      String descricao,
      String ncm,
      String cfop,
      String csosn,
      String ean,
      BigDecimal quantidade,
      BigDecimal valorUnitario,
      BigDecimal valorTotal) {}

  record Pagamento(String tipoPagamento, BigDecimal valor) {}

  record ApiKeyRequest(String name) {}

  record CancelRequest(String invoiceId, String motivo) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ProjectResponse(String id) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record CertificateResponse(String validUntil) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ApiKeyResponse(String key) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record EmitResponse(String invoiceId, String status) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record StatusResponse(
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

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ErrorResponse(String error, String message) {}
}
