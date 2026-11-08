package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.PixUseCasePort;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.outbound.PixChargeRepositoryPort;
import com.binitech.pdv.application.ports.outbound.PixGatewayPort;
import com.binitech.pdv.application.ports.outbound.PixGatewayPort.ChargeState;
import com.binitech.pdv.application.ports.outbound.PixSettingsRepositoryPort;
import com.binitech.pdv.domain.Money;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.PixCharge;
import com.binitech.pdv.domain.PixSettings;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.PixChargeStatus;
import com.binitech.pdv.utils.enums.Role;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

public class PixUseCaseImpl implements PixUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(PixUseCaseImpl.class);
  private static final String UNAVAILABLE = "Pix integrado indisponível.";
  private static final String NOT_FOUND = "Cobrança Pix não encontrada.";
  private static final int MAX_TOKEN_LENGTH = 500;

  public record Options(
      boolean enabled,
      Duration expiration,
      Duration statusCheckInterval,
      String notificationBaseUrl,
      Clock clock) {}

  private final Options options;
  private final PixSettingsRepositoryPort settingsRepository;
  private final PixChargeRepositoryPort chargeRepository;
  private final PixGatewayPort gateway;
  private final SaleUseCasePort saleUseCase;

  public PixUseCaseImpl(
      Options options,
      PixSettingsRepositoryPort settingsRepository,
      PixChargeRepositoryPort chargeRepository,
      PixGatewayPort gateway,
      SaleUseCasePort saleUseCase) {
    this.options = options;
    this.settingsRepository = settingsRepository;
    this.chargeRepository = chargeRepository;
    this.gateway = gateway;
    this.saleUseCase = saleUseCase;
  }

  @Override
  public PixSettings getSettings(Operator operator) {
    requireEnabled();
    return settingsOf(operator.tenantId());
  }

  @Override
  public PixSettings updateSettings(Operator operator, SettingsUpdate update) {
    requireEnabled();
    if (!isAdmin(operator.role())) {
      throw new AccessDeniedException("Só o administrador da loja altera o Pix.");
    }
    PixSettings current = settingsOf(operator.tenantId());
    String token = current.accessToken();
    String accountName = current.accountName();
    String newToken = trimmed(update.accessToken());
    if (newToken != null) {
      if (newToken.length() > MAX_TOKEN_LENGTH) {
        throw new BusinessException("Access Token inválido.");
      }
      accountName = gateway.verifyAccount(newToken);
      token = newToken;
    }
    String secret = current.webhookSecret();
    String newSecret = trimmed(update.webhookSecret());
    if (newSecret != null) {
      if (newSecret.length() > MAX_TOKEN_LENGTH) {
        throw new BusinessException("Assinatura secreta inválida.");
      }
      secret = newSecret;
    }
    boolean enabled = update.enabled() == null ? current.enabled() : update.enabled();
    if (enabled && (token == null || token.isBlank())) {
      throw new BusinessException("Informe o Access Token do Mercado Pago para ligar o Pix.");
    }
    PixSettings saved =
        settingsRepository.save(
            new PixSettings(operator.tenantId(), enabled, token, secret, accountName, now()));
    if (log.isInfoEnabled()) {
      log.info(
          "Pix atualizado: tenantId={} ativo={}",
          LogSanitizer.maskId(operator.tenantId()),
          enabled);
    }
    return saved;
  }

  @Override
  public PixCharge createCharge(Operator operator, Sale saleRequest) {
    requireEnabled();
    PixSettings settings = settingsOf(operator.tenantId());
    if (!settings.active()) {
      throw new BusinessException("O Pix integrado não está ligado nesta loja.");
    }
    double amount = pixAmount(saleRequest);
    if (Money.compare(amount, 0) <= 0) {
      throw new BusinessException("Informe o valor a receber por Pix.");
    }
    Sale request = copyRequest(saleRequest);
    saleUseCase.previewSale(copyRequest(saleRequest), operator.tenantId());

    LocalDateTime createdAt = now();
    PixCharge charge = new PixCharge();
    charge.setId(UUID.randomUUID().toString());
    charge.setTenantId(operator.tenantId());
    charge.setUserId(operator.userId());
    charge.setAmount(amount);
    charge.setStatus(PixChargeStatus.PENDING);
    charge.setCreatedAt(createdAt);
    charge.setExpiresAt(createdAt.plus(options.expiration()));
    charge.setSaleRequest(request);

    PixGatewayPort.CreatedCharge created =
        gateway.createCharge(
            settings.accessToken(),
            new PixGatewayPort.ChargeRequest(
                charge.getId(),
                BigDecimal.valueOf(amount),
                "Compra no caixa",
                charge.getId(),
                charge.getExpiresAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                notificationUrl(charge.getId())));
    charge.setProviderPaymentId(created.providerPaymentId());
    charge.setQrCode(created.qrCode());
    charge.setQrCodeBase64(created.qrCodeBase64());
    PixCharge saved = chargeRepository.insert(charge);
    if (log.isInfoEnabled()) {
      log.info(
          "Cobrança Pix criada: id={} tenantId={} valor={}",
          LogSanitizer.maskId(saved.getId()),
          LogSanitizer.maskId(operator.tenantId()),
          amount);
    }
    return saved;
  }

  @Override
  public PixCharge getCharge(Operator operator, String chargeId) {
    requireEnabled();
    PixCharge charge = ownedCharge(operator, chargeId);
    if (charge.isPending() && dueForCheck(charge)) {
      return refresh(charge, settingsOf(charge.getTenantId()));
    }
    return charge;
  }

  @Override
  public PixCharge cancelCharge(Operator operator, String chargeId) {
    requireEnabled();
    PixCharge charge = ownedCharge(operator, chargeId);
    if (!charge.isPending()) {
      return charge;
    }
    PixSettings settings = settingsOf(charge.getTenantId());
    PixCharge checked = refresh(charge, settings);
    if (!checked.isPending()) {
      return checked;
    }
    cancelAtProvider(settings, charge);
    return finish(charge, PixChargeStatus.CANCELLED);
  }

  @Override
  public void handleNotification(Notification notification) {
    if (!options.enabled() || notification.chargeId() == null) {
      return;
    }
    PixCharge charge = chargeRepository.findById(notification.chargeId()).orElse(null);
    if (charge == null || !charge.isPending()) {
      return;
    }
    PixSettings settings = settingsOf(charge.getTenantId());
    String secret = settings.webhookSecret();
    if (secret != null
        && !secret.isBlank()
        && !gateway.isValidSignature(
            secret, notification.dataId(), notification.requestId(), notification.signature())) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Notificação Pix com assinatura inválida: cobrança={}",
            LogSanitizer.maskId(charge.getId()));
      }
      return;
    }
    if (notification.dataId() != null
        && !notification.dataId().equals(charge.getProviderPaymentId())) {
      return;
    }
    refresh(charge, settings);
  }

  private PixCharge refresh(PixCharge charge, PixSettings settings) {
    LocalDateTime checkedAt = now();
    chargeRepository.markChecked(charge.getId(), checkedAt);
    charge.setLastCheckedAt(checkedAt);
    ChargeState state;
    try {
      state = gateway.fetchState(settings.accessToken(), charge.getProviderPaymentId());
    } catch (RuntimeException e) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Não foi possível consultar o Pix: cobrança={} erro={}",
            LogSanitizer.maskId(charge.getId()),
            e.getMessage());
      }
      return charge;
    }
    return switch (state) {
      case APPROVED -> complete(charge);
      case CANCELLED, REJECTED -> finish(charge, PixChargeStatus.EXPIRED);
      default -> expireIfLate(charge, settings);
    };
  }

  private PixCharge expireIfLate(PixCharge charge, PixSettings settings) {
    if (now().isBefore(charge.getExpiresAt())) {
      return charge;
    }
    cancelAtProvider(settings, charge);
    return finish(charge, PixChargeStatus.EXPIRED);
  }

  private PixCharge complete(PixCharge charge) {
    if (!chargeRepository.transition(
        charge.getId(), PixChargeStatus.PENDING, PixChargeStatus.PROCESSING)) {
      return chargeRepository.findById(charge.getId()).orElse(charge);
    }
    charge.setStatus(PixChargeStatus.PAID);
    charge.setPaidAt(now());
    Sale sale = copyRequest(charge.getSaleRequest());
    sale.setSkipStockValidation(true);
    try {
      Sale created = saleUseCase.createSale(sale, charge.getUserId(), charge.getTenantId());
      charge.setSaleId(created.getId());
      if (log.isInfoEnabled()) {
        log.info(
            "Pix confirmado e venda registrada: cobrança={} venda={}",
            LogSanitizer.maskId(charge.getId()),
            LogSanitizer.maskId(created.getId()));
      }
    } catch (RuntimeException e) {
      charge.setFailureReason(
          "O Pix foi recebido, mas a venda não foi registrada: " + e.getMessage());
      if (log.isErrorEnabled()) {
        log.error(
            "Pix recebido sem venda registrada: cobrança={} erro={}",
            LogSanitizer.maskId(charge.getId()),
            e.getMessage());
      }
    }
    return chargeRepository.save(charge);
  }

  private PixCharge finish(PixCharge charge, PixChargeStatus status) {
    if (chargeRepository.transition(charge.getId(), PixChargeStatus.PENDING, status)) {
      charge.setStatus(status);
      return charge;
    }
    return chargeRepository.findById(charge.getId()).orElse(charge);
  }

  private void cancelAtProvider(PixSettings settings, PixCharge charge) {
    try {
      gateway.cancel(settings.accessToken(), charge.getProviderPaymentId());
    } catch (RuntimeException e) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Não foi possível cancelar o Pix no provedor: cobrança={} erro={}",
            LogSanitizer.maskId(charge.getId()),
            e.getMessage());
      }
    }
  }

  private boolean dueForCheck(PixCharge charge) {
    return charge.getLastCheckedAt() == null
        || !now().isBefore(charge.getLastCheckedAt().plus(options.statusCheckInterval()));
  }

  private PixCharge ownedCharge(Operator operator, String chargeId) {
    return chargeRepository
        .findByIdAndTenantId(chargeId, operator.tenantId())
        .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
  }

  private PixSettings settingsOf(String tenantId) {
    return settingsRepository.findByTenantId(tenantId).orElse(PixSettings.empty(tenantId));
  }

  private void requireEnabled() {
    if (!options.enabled()) {
      throw new ResourceNotFoundException(UNAVAILABLE);
    }
  }

  private String notificationUrl(String chargeId) {
    String base = options.notificationBaseUrl();
    if (base == null || base.isBlank() || !base.startsWith("https://")) {
      return null;
    }
    String trimmed = base;
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed + "/webhooks/mercadopago/pix?charge=" + chargeId;
  }

  private static double pixAmount(Sale sale) {
    if (sale.getPayments() == null) {
      return 0;
    }
    return Money.sum(
        sale.getPayments().stream()
            .filter(p -> p.getMethod() == PaymentMethod.PIX)
            .map(Payment::getAmount));
  }

  private static Sale copyRequest(Sale source) {
    Sale copy = new Sale();
    ArrayList<SaleItem> items = new ArrayList<>();
    if (source.getItems() != null) {
      for (SaleItem item : source.getItems()) {
        SaleItem clone = new SaleItem();
        clone.setProductId(item.getProductId());
        clone.setQuantity(item.getQuantity());
        items.add(clone);
      }
    }
    copy.setItems(items);
    ArrayList<Payment> payments = new ArrayList<>();
    if (source.getPayments() != null) {
      for (Payment payment : source.getPayments()) {
        payments.add(new Payment(payment.getMethod(), payment.getAmount()));
      }
    }
    copy.setPayments(payments);
    copy.setCustomerName(source.getCustomerName());
    copy.setCustomerPhone(source.getCustomerPhone());
    copy.setCustomerCpf(source.getCustomerCpf());
    copy.setDiscount(source.getDiscount());
    copy.setSurcharge(source.getSurcharge());
    copy.setSkipStockValidation(source.isSkipStockValidation());
    return copy;
  }

  private LocalDateTime now() {
    return LocalDateTime.now(options.clock().withZone(ZoneId.systemDefault()));
  }

  private static String trimmed(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static boolean isAdmin(Role role) {
    return role == Role.ADMIN || role == Role.TENANT_ADMIN || role == Role.SUPER_ADMIN;
  }
}
