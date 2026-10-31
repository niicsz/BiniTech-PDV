package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.application.ports.inbound.FiscalUseCasePort;
import com.binitech.pdv.application.ports.inbound.FiscalUseCasePort.ProfileCommand;
import com.binitech.pdv.application.ports.outbound.AddressLookupPort;
import com.binitech.pdv.application.ports.outbound.AddressLookupPort.AddressInfo;
import com.binitech.pdv.domain.FiscalDocument;
import com.binitech.pdv.domain.FiscalProfile;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FiscalController {

  private static final long MAX_CERTIFICATE_BYTES = 50L * 1024;

  private final FiscalUseCasePort fiscalUseCase;
  private final AuthenticatedUserProvider authenticatedUserProvider;
  private final AddressLookupPort addressLookup;

  public FiscalController(
      FiscalUseCasePort fiscalUseCase,
      AuthenticatedUserProvider authenticatedUserProvider,
      AddressLookupPort addressLookup) {
    this.fiscalUseCase = fiscalUseCase;
    this.authenticatedUserProvider = authenticatedUserProvider;
    this.addressLookup = addressLookup;
  }

  @GetMapping("/api/fiscal/cep/{cep}")
  public ResponseEntity<AddressInfo> lookupCep(@PathVariable String cep) {
    return addressLookup
        .findByCep(cep)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/api/fiscal/profile")
  public ResponseEntity<FiscalProfileDTO> getProfile() {
    return fiscalUseCase
        .getProfile(authenticatedUserProvider.getTenantId())
        .map(this::toDto)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PutMapping("/api/fiscal/profile")
  public ResponseEntity<FiscalProfileDTO> saveProfile(@RequestBody ProfileRequest request) {
    requireStoreAdmin();
    FiscalProfile saved =
        fiscalUseCase.saveProfile(
            authenticatedUserProvider.getTenantId(),
            new ProfileCommand(
                request.cnpj(),
                request.razaoSocial(),
                request.inscricaoEstadual(),
                request.regimeTributario(),
                request.codigoMunicipio(),
                request.email(),
                request.telefone(),
                request.logradouro(),
                request.numero(),
                request.complemento(),
                request.bairro(),
                request.cidade(),
                request.uf(),
                request.cep(),
                request.serieNfce(),
                request.cscId(),
                request.cscToken(),
                request.defaultNcm(),
                request.defaultCfop(),
                request.defaultCsosn(),
                request.autoIssue()));
    return ResponseEntity.ok(toDto(saved));
  }

  @PostMapping(value = "/api/fiscal/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<FiscalProfileDTO> uploadCertificate(
      @RequestParam("file") MultipartFile file, @RequestParam("password") String password)
      throws IOException {
    requireStoreAdmin();
    if (file.getSize() > MAX_CERTIFICATE_BYTES) {
      throw new BusinessException("O certificado A1 deve ter no máximo 50 KB.");
    }
    FiscalProfile saved =
        fiscalUseCase.uploadCertificate(
            authenticatedUserProvider.getTenantId(), file.getBytes(), password);
    return ResponseEntity.ok(toDto(saved));
  }

  @PostMapping("/api/sales/{saleId}/nfce")
  public ResponseEntity<FiscalDocumentDTO> issueNfce(
      @PathVariable String saleId, @RequestBody(required = false) IssueRequest request) {
    FiscalDocument document =
        fiscalUseCase.issueNfce(
            authenticatedUserProvider.getTenantId(),
            saleId,
            request != null ? request.consumerCpf() : null);
    return ResponseEntity.ok(toDto(document));
  }

  @GetMapping("/api/sales/{saleId}/nfce")
  public ResponseEntity<FiscalDocumentDTO> getNfce(@PathVariable String saleId) {
    return fiscalUseCase
        .getNfce(authenticatedUserProvider.getTenantId(), saleId)
        .map(this::toDto)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PostMapping("/api/sales/{saleId}/nfce/cancel")
  public ResponseEntity<FiscalDocumentDTO> cancelNfce(
      @PathVariable String saleId, @Valid @RequestBody CancelRequest request) {
    requireStoreAdmin();
    FiscalDocument document =
        fiscalUseCase.cancelNfce(authenticatedUserProvider.getTenantId(), saleId, request.reason());
    return ResponseEntity.ok(toDto(document));
  }

  @GetMapping(value = "/api/sales/{saleId}/nfce/danfe", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> downloadDanfe(@PathVariable String saleId) {
    byte[] pdf = fiscalUseCase.downloadDanfe(authenticatedUserProvider.getTenantId(), saleId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"nfce-" + saleId + ".pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  private void requireStoreAdmin() {
    Role role = authenticatedUserProvider.getUserRole();
    if (role != Role.TENANT_ADMIN && role != Role.ADMIN) {
      throw new AccessDeniedException("Apenas o administrador da loja pode alterar dados fiscais.");
    }
  }

  private FiscalProfileDTO toDto(FiscalProfile profile) {
    return new FiscalProfileDTO(
        profile.getCnpj(),
        profile.getRazaoSocial(),
        profile.getInscricaoEstadual(),
        profile.getRegimeTributario(),
        profile.getCodigoMunicipio(),
        profile.getEmail(),
        profile.getTelefone(),
        profile.getLogradouro(),
        profile.getNumero(),
        profile.getComplemento(),
        profile.getBairro(),
        profile.getCidade(),
        profile.getUf(),
        profile.getCep(),
        profile.getSerieNfce(),
        profile.getCscId(),
        profile.isCscConfigured(),
        profile.getDefaultNcm(),
        profile.getDefaultCfop(),
        profile.getDefaultCsosn(),
        profile.isAutoIssue(),
        profile.getCertificateValidUntil(),
        profile.getEnvironment(),
        fiscalUseCase.isReadyToIssue(profile));
  }

  private FiscalDocumentDTO toDto(FiscalDocument document) {
    return new FiscalDocumentDTO(
        document.getSaleId(),
        document.getStatus() != null ? document.getStatus().name() : null,
        document.getNumero(),
        document.getSerie(),
        document.getChaveAcesso(),
        document.getProtocolo(),
        document.getCodigoStatus(),
        document.getMotivo(),
        document.getQrCode(),
        document.getUrlChave(),
        document.getValorTotal(),
        document.getConsumerCpf(),
        document.getEnvironment(),
        document.getIssuedAt(),
        document.getCancelledAt());
  }

  public record ProfileRequest(
      String cnpj,
      String razaoSocial,
      String inscricaoEstadual,
      String regimeTributario,
      String codigoMunicipio,
      String email,
      String telefone,
      String logradouro,
      String numero,
      String complemento,
      String bairro,
      String cidade,
      String uf,
      String cep,
      Integer serieNfce,
      String cscId,
      String cscToken,
      String defaultNcm,
      String defaultCfop,
      String defaultCsosn,
      boolean autoIssue) {}

  public record IssueRequest(String consumerCpf) {}

  public record CancelRequest(@NotBlank @Size(min = 15, max = 255) String reason) {}

  public record FiscalProfileDTO(
      String cnpj,
      String razaoSocial,
      String inscricaoEstadual,
      String regimeTributario,
      String codigoMunicipio,
      String email,
      String telefone,
      String logradouro,
      String numero,
      String complemento,
      String bairro,
      String cidade,
      String uf,
      String cep,
      int serieNfce,
      String cscId,
      boolean cscConfigured,
      String defaultNcm,
      String defaultCfop,
      String defaultCsosn,
      boolean autoIssue,
      LocalDate certificateValidUntil,
      int environment,
      boolean readyToIssue) {}

  public record FiscalDocumentDTO(
      String saleId,
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
      String consumerCpf,
      int environment,
      LocalDateTime issuedAt,
      LocalDateTime cancelledAt) {}
}
