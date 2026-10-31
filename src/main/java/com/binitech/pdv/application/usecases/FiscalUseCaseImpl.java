package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.FiscalUseCasePort;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.outbound.FiscalDocumentRepositoryPort;
import com.binitech.pdv.application.ports.outbound.FiscalEmitterPort;
import com.binitech.pdv.application.ports.outbound.FiscalEmitterPort.Address;
import com.binitech.pdv.application.ports.outbound.FiscalEmitterPort.Consumer;
import com.binitech.pdv.application.ports.outbound.FiscalEmitterPort.IssuerData;
import com.binitech.pdv.application.ports.outbound.FiscalEmitterPort.NfceItem;
import com.binitech.pdv.application.ports.outbound.FiscalEmitterPort.NfcePayment;
import com.binitech.pdv.application.ports.outbound.FiscalEmitterPort.NfceRequest;
import com.binitech.pdv.application.ports.outbound.FiscalEmitterPort.NfceStatus;
import com.binitech.pdv.application.ports.outbound.FiscalProfileRepositoryPort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.config.SecretCipher;
import com.binitech.pdv.domain.FiscalDocument;
import com.binitech.pdv.domain.FiscalProfile;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.FiscalDocumentStatus;
import com.binitech.pdv.utils.enums.PaymentMethod;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FiscalUseCaseImpl implements FiscalUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(FiscalUseCaseImpl.class);
  private static final Pattern CNPJ = Pattern.compile("^[0-9A-Z]{12}\\d{2}$");
  private static final Pattern DIGITS_8 = Pattern.compile("^\\d{8}$");
  private static final Pattern DIGITS_7 = Pattern.compile("^\\d{7}$");
  private static final Pattern DIGITS_4 = Pattern.compile("^\\d{4}$");
  private static final Pattern DIGITS_3 = Pattern.compile("^\\d{3}$");
  private static final Pattern UF = Pattern.compile("^[A-Z]{2}$");
  private static final Pattern GTIN = Pattern.compile("^(\\d{8}|\\d{12,14})$");
  private static final Set<String> SUPPORTED_REGIMES = Set.of("2", "3");
  private static final String DEFAULT_CFOP = "5102";
  private static final String DEFAULT_CSOSN = "102";
  private static final String PAYMENT_CASH = "01";
  private static final String PAYMENT_STORE_CREDIT = "05";
  private static final int MIN_CANCEL_REASON = 15;
  private static final int MAX_CANCEL_REASON = 255;
  private static final int CANCEL_CONFIRMATION_MINUTES = 10;

  private final FiscalProfileRepositoryPort profileRepository;
  private final FiscalDocumentRepositoryPort documentRepository;
  private final FiscalEmitterPort emitter;
  private final SaleUseCasePort saleUseCase;
  private final ProductRepositoryPort productRepository;
  private final TenantRepositoryPort tenantRepository;
  private final SecretCipher secretCipher;
  private final Clock clock;

  public FiscalUseCaseImpl(
      FiscalProfileRepositoryPort profileRepository,
      FiscalDocumentRepositoryPort documentRepository,
      FiscalEmitterPort emitter,
      SaleUseCasePort saleUseCase,
      ProductRepositoryPort productRepository,
      TenantRepositoryPort tenantRepository,
      SecretCipher secretCipher,
      Clock clock) {
    this.profileRepository = profileRepository;
    this.documentRepository = documentRepository;
    this.emitter = emitter;
    this.saleUseCase = saleUseCase;
    this.productRepository = productRepository;
    this.tenantRepository = tenantRepository;
    this.secretCipher = secretCipher;
    this.clock = clock;
  }

  @Override
  public Optional<FiscalProfile> getProfile(String tenantId) {
    return profileRepository.findByTenantId(tenantId);
  }

  @Override
  public FiscalProfile saveProfile(String tenantId, ProfileCommand command) {
    requireEmitterAvailable();
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
    LocalDateTime now = LocalDateTime.now(clock);
    FiscalProfile profile =
        profileRepository.findByTenantId(tenantId).orElseGet(FiscalProfile::new);
    applyCommand(profile, command);
    profile.setTenantId(tenantId);
    boolean cscProvided = hasText(command.cscToken());
    if (cscProvided) {
      profile.setCscConfigured(true);
    }
    if (profile.getCreatedAt() == null) {
      profile.setCreatedAt(now);
    }
    profile.setUpdatedAt(now);

    IssuerData issuer = toIssuer(tenant, profile, cscProvided ? command.cscToken().trim() : null);
    if (!hasText(profile.getProviderProjectId())) {
      profile.setProviderProjectId(emitter.createIssuer(issuer));
    } else {
      emitter.updateIssuer(profile.getProviderProjectId(), issuer);
    }
    if (!hasText(profile.getProviderApiKeyEncrypted())) {
      String apiKey = emitter.createIssuerApiKey(profile.getProviderProjectId());
      profile.setProviderApiKeyEncrypted(secretCipher.encrypt(apiKey));
    }
    profile.setEnvironment(emitter.environment());

    FiscalProfile saved = profileRepository.save(profile);
    if (log.isInfoEnabled()) {
      log.info("Perfil fiscal salvo: tenantId={}", LogSanitizer.maskId(tenantId));
    }
    return saved;
  }

  @Override
  public FiscalProfile uploadCertificate(String tenantId, byte[] certificate, String password) {
    requireEmitterAvailable();
    FiscalProfile profile =
        profileRepository
            .findByTenantId(tenantId)
            .filter(p -> hasText(p.getProviderProjectId()))
            .orElseThrow(
                () ->
                    new BusinessException(
                        "Salve os dados fiscais da loja antes de enviar o certificado."));
    if (certificate == null || certificate.length == 0) {
      throw new BusinessException("Selecione o arquivo do certificado A1 (.pfx ou .p12).");
    }
    if (!hasText(password)) {
      throw new BusinessException("Informe a senha do certificado A1.");
    }
    LocalDate validUntil =
        emitter.uploadCertificate(profile.getProviderProjectId(), certificate, password);
    profile.setCertificateValidUntil(validUntil);
    profile.setUpdatedAt(LocalDateTime.now(clock));
    FiscalProfile saved = profileRepository.save(profile);
    if (log.isInfoEnabled()) {
      log.info(
          "Certificado A1 enviado: tenantId={} validoAte={}",
          LogSanitizer.maskId(tenantId),
          validUntil);
    }
    return saved;
  }

  @Override
  public boolean isReadyToIssue(FiscalProfile profile) {
    return profile != null
        && hasText(profile.getProviderProjectId())
        && hasText(profile.getProviderApiKeyEncrypted())
        && profile.isCscConfigured()
        && profile.getCertificateValidUntil() != null
        && !profile.getCertificateValidUntil().isBefore(LocalDate.now(clock));
  }

  @Override
  public FiscalDocument issueNfce(String tenantId, String saleId, String consumerCpf) {
    requireEmitterAvailable();
    FiscalProfile profile =
        profileRepository
            .findByTenantId(tenantId)
            .filter(this::isReadyToIssue)
            .orElseThrow(
                () ->
                    new BusinessException(
                        "Configure os dados fiscais, o CSC e um certificado A1 válido antes de"
                            + " emitir NFC-e."));
    Optional<FiscalDocument> existing =
        documentRepository.findBySaleIdAndTenantId(saleId, tenantId);
    if (existing.isPresent() && existing.get().getStatus() != FiscalDocumentStatus.ERROR) {
      return refresh(profile, existing.get());
    }

    String cpf = normalizeConsumerCpf(consumerCpf);
    Sale sale = saleUseCase.findById(saleId, tenantId);
    NfceRequest request = buildRequest(profile, sale, cpf);
    String idempotencyKey = existing.isPresent() ? saleId + "-" + clock.millis() : "nfce-" + saleId;
    String invoiceId = emitter.issueNfce(apiKey(profile), request, idempotencyKey);

    LocalDateTime now = LocalDateTime.now(clock);
    FiscalDocument document = existing.orElseGet(FiscalDocument::new);
    document.setTenantId(tenantId);
    document.setSaleId(saleId);
    document.setProviderInvoiceId(invoiceId);
    document.setStatus(FiscalDocumentStatus.QUEUED);
    document.setCodigoStatus(null);
    document.setMotivo(null);
    document.setValorTotal(sumItems(request.items()));
    document.setConsumerCpf(cpf);
    document.setEnvironment(profile.getEnvironment());
    if (document.getCreatedAt() == null) {
      document.setCreatedAt(now);
    }
    document.setUpdatedAt(now);
    FiscalDocument saved = documentRepository.save(document);
    if (log.isInfoEnabled()) {
      log.info(
          "NFC-e enviada para emissão: tenantId={} saleId={}",
          LogSanitizer.maskId(tenantId),
          LogSanitizer.maskId(saleId));
    }
    return refreshQuietly(profile, saved);
  }

  @Override
  public Optional<FiscalDocument> getNfce(String tenantId, String saleId) {
    Optional<FiscalDocument> document =
        documentRepository.findBySaleIdAndTenantId(saleId, tenantId);
    if (document.isEmpty() || !emitter.isConfigured()) {
      return document;
    }
    Optional<FiscalProfile> profile = profileRepository.findByTenantId(tenantId);
    if (profile.isEmpty() || !hasText(profile.get().getProviderApiKeyEncrypted())) {
      return document;
    }
    return Optional.of(refresh(profile.get(), document.get()));
  }

  @Override
  public FiscalDocument cancelNfce(String tenantId, String saleId, String reason) {
    requireEmitterAvailable();
    String motivo = reason == null ? "" : reason.trim();
    if (motivo.length() < MIN_CANCEL_REASON || motivo.length() > MAX_CANCEL_REASON) {
      throw new BusinessException(
          "Informe o motivo do cancelamento com 15 a 255 caracteres (exigência da SEFAZ).");
    }
    FiscalProfile profile = requireProfileWithKey(tenantId);
    FiscalDocument document = requireDocument(tenantId, saleId);
    if (document.getStatus() != FiscalDocumentStatus.ISSUED) {
      throw new BusinessException("Só é possível cancelar uma NFC-e autorizada.");
    }
    emitter.cancel(apiKey(profile), document.getProviderInvoiceId(), motivo);
    document.setStatus(FiscalDocumentStatus.CANCELLING);
    document.setMotivo(motivo);
    document.setUpdatedAt(LocalDateTime.now(clock));
    FiscalDocument saved = documentRepository.save(document);
    if (log.isInfoEnabled()) {
      log.info(
          "Cancelamento de NFC-e solicitado: tenantId={} saleId={}",
          LogSanitizer.maskId(tenantId),
          LogSanitizer.maskId(saleId));
    }
    return refreshQuietly(profile, saved);
  }

  @Override
  public byte[] downloadDanfe(String tenantId, String saleId) {
    requireEmitterAvailable();
    FiscalProfile profile = requireProfileWithKey(tenantId);
    FiscalDocument document = requireDocument(tenantId, saleId);
    if (document.getStatus() != FiscalDocumentStatus.ISSUED
        && document.getStatus() != FiscalDocumentStatus.CANCELLING
        && document.getStatus() != FiscalDocumentStatus.CANCELLED) {
      throw new BusinessException("A NFC-e ainda não foi autorizada pela SEFAZ.");
    }
    return emitter.downloadDanfe(apiKey(profile), document.getProviderInvoiceId());
  }

  private FiscalDocument refreshQuietly(FiscalProfile profile, FiscalDocument document) {
    try {
      return refresh(profile, document);
    } catch (BusinessException e) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Status da NFC-e indisponível no momento: saleId={} erro={}",
            LogSanitizer.maskId(document.getSaleId()),
            e.getMessage());
      }
      return document;
    }
  }

  private FiscalDocument refresh(FiscalProfile profile, FiscalDocument document) {
    FiscalDocumentStatus current = document.getStatus();
    if (current != FiscalDocumentStatus.QUEUED
        && current != FiscalDocumentStatus.PROCESSING
        && current != FiscalDocumentStatus.CANCELLING) {
      return document;
    }
    NfceStatus status = emitter.fetchStatus(apiKey(profile), document.getProviderInvoiceId());
    LocalDateTime now = LocalDateTime.now(clock);
    FiscalDocumentStatus next = nextStatus(document, status, now);
    if (next == current && !"issued".equals(status.status())) {
      return document;
    }
    document.setStatus(next);
    document.setNumero(status.numero());
    document.setSerie(status.serie());
    document.setChaveAcesso(status.chaveAcesso());
    document.setProtocolo(status.protocolo());
    document.setCodigoStatus(status.codigoStatus());
    if (next != FiscalDocumentStatus.CANCELLING) {
      document.setMotivo(hasText(status.motivo()) ? status.motivo() : status.errorMessage());
    }
    document.setQrCode(status.qrCode());
    document.setUrlChave(status.urlChave());
    if (status.valorTotal() != null) {
      document.setValorTotal(status.valorTotal());
    }
    if (next == FiscalDocumentStatus.ISSUED && document.getIssuedAt() == null) {
      document.setIssuedAt(now);
    }
    if (next == FiscalDocumentStatus.CANCELLED && document.getCancelledAt() == null) {
      document.setCancelledAt(now);
    }
    document.setUpdatedAt(now);
    return documentRepository.save(document);
  }

  private FiscalDocumentStatus nextStatus(
      FiscalDocument document, NfceStatus status, LocalDateTime now) {
    String providerStatus = status.status() == null ? "" : status.status().toLowerCase(Locale.ROOT);
    if (document.getStatus() == FiscalDocumentStatus.CANCELLING) {
      if ("cancelled".equals(providerStatus)) {
        return FiscalDocumentStatus.CANCELLED;
      }
      boolean expired =
          document.getUpdatedAt() != null
              && document.getUpdatedAt().plusMinutes(CANCEL_CONFIRMATION_MINUTES).isBefore(now);
      return expired ? FiscalDocumentStatus.ISSUED : FiscalDocumentStatus.CANCELLING;
    }
    return switch (providerStatus) {
      case "issued" -> FiscalDocumentStatus.ISSUED;
      case "error", "inutilized" -> FiscalDocumentStatus.ERROR;
      case "cancelled" -> FiscalDocumentStatus.CANCELLED;
      case "processing" -> FiscalDocumentStatus.PROCESSING;
      default -> FiscalDocumentStatus.QUEUED;
    };
  }

  private NfceRequest buildRequest(FiscalProfile profile, Sale sale, String consumerCpf) {
    if (sale.getItems() == null || sale.getItems().isEmpty()) {
      throw new BusinessException("A venda não possui itens para emitir NFC-e.");
    }
    List<NfceItem> items = new ArrayList<>();
    for (SaleItem saleItem : sale.getItems()) {
      items.add(toItem(profile, sale.getTenantId(), saleItem));
    }
    BigDecimal total = sumItems(items);
    Consumer consumer =
        consumerCpf == null ? null : new Consumer(consumerCpf, sale.getCustomerName());
    return new NfceRequest(consumer, items, payments(sale.getPayments(), total));
  }

  private NfceItem toItem(FiscalProfile profile, String tenantId, SaleItem saleItem) {
    Product product =
        Optional.ofNullable(saleItem.getProductId())
            .flatMap(productRepository::findById)
            .filter(p -> tenantId == null || tenantId.equals(p.getTenantId()))
            .orElse(null);
    String description =
        hasText(saleItem.getProductDescription())
            ? saleItem.getProductDescription()
            : product != null ? product.getDescription() : "Produto";
    String ncm = firstText(product != null ? product.getNcm() : null, profile.getDefaultNcm());
    if (ncm == null || !DIGITS_8.matcher(ncm).matches()) {
      throw new BusinessException(
          "O produto \""
              + description
              + "\" está sem NCM. Cadastre o NCM (8 dígitos) no produto"
              + " ou um NCM padrão nos dados fiscais.");
    }
    String cfop =
        firstText(
            product != null ? product.getCfop() : null, profile.getDefaultCfop(), DEFAULT_CFOP);
    String csosn =
        firstText(
            product != null ? product.getCsosn() : null, profile.getDefaultCsosn(), DEFAULT_CSOSN);
    String barcode = product != null ? product.getBarcode() : null;
    String ean = barcode != null && GTIN.matcher(barcode).matches() ? barcode : null;
    BigDecimal quantity = BigDecimal.valueOf(saleItem.getQuantity());
    BigDecimal unitPrice = money(saleItem.getUnitPrice());
    BigDecimal lineTotal =
        saleItem.getSubtotal() > 0
            ? money(saleItem.getSubtotal())
            : money(unitPrice.multiply(quantity));
    return new NfceItem(
        product != null ? firstText(barcode, product.getId()) : saleItem.getProductId(),
        description,
        ncm,
        cfop,
        csosn,
        ean,
        quantity,
        unitPrice,
        lineTotal);
  }

  static List<NfcePayment> payments(List<Payment> salePayments, BigDecimal total) {
    List<String> types = new ArrayList<>();
    List<BigDecimal> amounts = new ArrayList<>();
    if (salePayments != null) {
      for (Payment payment : salePayments) {
        BigDecimal amount = money(payment.getAmount());
        if (amount.signum() > 0) {
          types.add(paymentType(payment.getMethod()));
          amounts.add(amount);
        }
      }
    }
    BigDecimal paid = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal excess = paid.subtract(total);
    for (int i = amounts.size() - 1; i >= 0 && excess.signum() > 0; i--) {
      if (PAYMENT_CASH.equals(types.get(i))) {
        BigDecimal reduction = excess.min(amounts.get(i));
        amounts.set(i, amounts.get(i).subtract(reduction));
        excess = excess.subtract(reduction);
      }
    }
    for (int i = amounts.size() - 1; i >= 0 && excess.signum() > 0; i--) {
      BigDecimal reduction = excess.min(amounts.get(i));
      amounts.set(i, amounts.get(i).subtract(reduction));
      excess = excess.subtract(reduction);
    }
    List<NfcePayment> result = new ArrayList<>();
    for (int i = 0; i < amounts.size(); i++) {
      if (amounts.get(i).signum() > 0) {
        result.add(new NfcePayment(types.get(i), amounts.get(i)));
      }
    }
    BigDecimal covered =
        result.stream().map(NfcePayment::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (covered.compareTo(total) < 0) {
      result.add(new NfcePayment(PAYMENT_STORE_CREDIT, total.subtract(covered)));
    }
    return result;
  }

  private static String paymentType(PaymentMethod method) {
    if (method == null) {
      return "99";
    }
    return switch (method) {
      case CASH -> PAYMENT_CASH;
      case CREDIT_CARD -> "03";
      case DEBIT_CARD -> "04";
      case PIX -> "17";
      case CREDIARIO -> PAYMENT_STORE_CREDIT;
    };
  }

  private void applyCommand(FiscalProfile profile, ProfileCommand command) {
    String cnpj = upperAlnum(command.cnpj());
    if (cnpj == null || !CNPJ.matcher(cnpj).matches() || !validCnpj(cnpj)) {
      throw new BusinessException("CNPJ inválido.");
    }
    if (!hasText(command.razaoSocial())) {
      throw new BusinessException("Informe a razão social.");
    }
    String regime = command.regimeTributario() == null ? "" : command.regimeTributario().trim();
    if (!SUPPORTED_REGIMES.contains(regime)) {
      throw new BusinessException(
          "Por enquanto a NFC-e está disponível para empresas do Simples Nacional e MEI.");
    }
    String ie = digits(command.inscricaoEstadual());
    if (ie == null || ie.length() < 2) {
      throw new BusinessException("Informe a inscrição estadual (obrigatória para NFC-e).");
    }
    String municipio = digits(command.codigoMunicipio());
    if (municipio == null || !DIGITS_7.matcher(municipio).matches()) {
      throw new BusinessException("Informe o código IBGE do município (7 dígitos).");
    }
    String uf = command.uf() == null ? null : command.uf().trim().toUpperCase(Locale.ROOT);
    if (uf == null || !UF.matcher(uf).matches()) {
      throw new BusinessException("Informe a UF com 2 letras.");
    }
    String cep = digits(command.cep());
    if (cep == null || !DIGITS_8.matcher(cep).matches()) {
      throw new BusinessException("Informe o CEP com 8 dígitos.");
    }
    if (!hasText(command.logradouro())
        || !hasText(command.bairro())
        || !hasText(command.cidade())) {
      throw new BusinessException("Informe logradouro, bairro e cidade.");
    }
    String defaultNcm = digits(command.defaultNcm());
    if (defaultNcm != null && !DIGITS_8.matcher(defaultNcm).matches()) {
      throw new BusinessException("O NCM padrão deve ter 8 dígitos.");
    }
    String defaultCfop = digits(command.defaultCfop());
    if (defaultCfop != null && !DIGITS_4.matcher(defaultCfop).matches()) {
      throw new BusinessException("O CFOP padrão deve ter 4 dígitos.");
    }
    String defaultCsosn = digits(command.defaultCsosn());
    if (defaultCsosn != null && !DIGITS_3.matcher(defaultCsosn).matches()) {
      throw new BusinessException("O CSOSN padrão deve ter 3 dígitos.");
    }
    int serie = command.serieNfce() == null ? 1 : command.serieNfce();
    if (serie < 1 || serie > 999) {
      throw new BusinessException("A série da NFC-e deve estar entre 1 e 999.");
    }
    if (hasText(command.cscToken()) && !hasText(command.cscId())) {
      throw new BusinessException("Informe o ID do CSC junto com o token.");
    }
    if (!profile.isCscConfigured() && !hasText(command.cscToken())) {
      throw new BusinessException("Informe o ID e o token do CSC obtidos no portal da SEFAZ.");
    }

    profile.setCnpj(cnpj);
    profile.setRazaoSocial(command.razaoSocial().trim());
    profile.setInscricaoEstadual(ie);
    profile.setRegimeTributario(regime);
    profile.setCodigoMunicipio(municipio);
    profile.setEmail(trimToNull(command.email()));
    profile.setTelefone(digits(command.telefone()));
    profile.setLogradouro(command.logradouro().trim());
    profile.setNumero(firstText(trimToNull(command.numero()), "SN"));
    profile.setComplemento(trimToNull(command.complemento()));
    profile.setBairro(command.bairro().trim());
    profile.setCidade(command.cidade().trim());
    profile.setUf(uf);
    profile.setCep(cep);
    profile.setSerieNfce(serie);
    if (hasText(command.cscId())) {
      profile.setCscId(command.cscId().trim());
    }
    profile.setDefaultNcm(defaultNcm);
    profile.setDefaultCfop(defaultCfop);
    profile.setDefaultCsosn(defaultCsosn);
    profile.setAutoIssue(command.autoIssue());
  }

  private IssuerData toIssuer(Tenant tenant, FiscalProfile profile, String cscToken) {
    return new IssuerData(
        tenant.getName(),
        profile.getCnpj(),
        profile.getRazaoSocial(),
        profile.getInscricaoEstadual(),
        profile.getRegimeTributario(),
        profile.getCodigoMunicipio(),
        profile.getEmail(),
        profile.getTelefone(),
        new Address(
            profile.getLogradouro(),
            profile.getNumero(),
            profile.getComplemento(),
            profile.getBairro(),
            profile.getCidade(),
            profile.getUf(),
            profile.getCep()),
        profile.getSerieNfce(),
        cscToken != null ? profile.getCscId() : null,
        cscToken);
  }

  private FiscalProfile requireProfileWithKey(String tenantId) {
    return profileRepository
        .findByTenantId(tenantId)
        .filter(p -> hasText(p.getProviderApiKeyEncrypted()))
        .orElseThrow(() -> new BusinessException("Configure os dados fiscais da loja."));
  }

  private FiscalDocument requireDocument(String tenantId, String saleId) {
    return documentRepository
        .findBySaleIdAndTenantId(saleId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("NFC-e", "saleId", saleId));
  }

  private void requireEmitterAvailable() {
    if (!emitter.isConfigured() || secretCipher == null) {
      throw new BusinessException("Emissão de NFC-e indisponível no momento. Fale com o suporte.");
    }
  }

  private String apiKey(FiscalProfile profile) {
    return secretCipher.decrypt(profile.getProviderApiKeyEncrypted());
  }

  private String normalizeConsumerCpf(String consumerCpf) {
    String cpf = digits(consumerCpf);
    if (cpf == null) {
      return null;
    }
    if (!validCpf(cpf)) {
      throw new BusinessException("CPF do consumidor inválido.");
    }
    return cpf;
  }

  static boolean validCpf(String cpf) {
    if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
      return false;
    }
    for (int length = 9; length <= 10; length++) {
      int sum = 0;
      for (int i = 0; i < length; i++) {
        sum += (cpf.charAt(i) - '0') * (length + 1 - i);
      }
      int digit = (sum * 10) % 11 % 10;
      if (digit != cpf.charAt(length) - '0') {
        return false;
      }
    }
    return true;
  }

  static boolean validCnpj(String cnpj) {
    if (cnpj.chars().distinct().count() == 1) {
      return false;
    }
    int[] firstWeights = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    int[] secondWeights = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    return checkDigit(cnpj, firstWeights) == cnpj.charAt(12) - '0'
        && checkDigit(cnpj, secondWeights) == cnpj.charAt(13) - '0';
  }

  private static int checkDigit(String cnpj, int[] weights) {
    int sum = 0;
    for (int i = 0; i < weights.length; i++) {
      sum += (cnpj.charAt(i) - '0') * weights[i];
    }
    int remainder = sum % 11;
    return remainder < 2 ? 0 : 11 - remainder;
  }

  private static BigDecimal sumItems(List<NfceItem> items) {
    return items.stream().map(NfceItem::valorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static BigDecimal money(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal money(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private static String upperAlnum(String value) {
    if (value == null) {
      return null;
    }
    String cleaned = value.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-Z]", "");
    return cleaned.isEmpty() ? null : cleaned;
  }

  private static String digits(String value) {
    if (value == null) {
      return null;
    }
    String cleaned = value.replaceAll("\\D", "");
    return cleaned.isEmpty() ? null : cleaned;
  }

  private static String trimToNull(String value) {
    return hasText(value) ? value.trim() : null;
  }

  private static String firstText(String... values) {
    for (String value : values) {
      if (hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
