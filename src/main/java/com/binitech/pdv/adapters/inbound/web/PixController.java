package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.PixApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.CreateSaleDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PixChargeDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PixChargeStatusDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PixSettingsDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.UpdatePixSettingsDTO;
import com.binitech.pdv.adapters.inbound.web.mapper.WebMapper;
import com.binitech.pdv.application.ports.inbound.PixUseCasePort;
import com.binitech.pdv.application.ports.inbound.PixUseCasePort.Operator;
import com.binitech.pdv.application.ports.inbound.PixUseCasePort.SettingsUpdate;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.domain.PixCharge;
import com.binitech.pdv.domain.PixSettings;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PixController implements PixApi {

  private final PixUseCasePort pix;
  private final SaleUseCasePort sales;
  private final WebMapper webMapper;
  private final AuthenticatedUserProvider userProvider;

  public PixController(
      PixUseCasePort pix,
      SaleUseCasePort sales,
      WebMapper webMapper,
      AuthenticatedUserProvider userProvider) {
    this.pix = pix;
    this.sales = sales;
    this.webMapper = webMapper;
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<PixSettingsDTO> getPixSettings() {
    return ResponseEntity.ok(toDto(pix.getSettings(operator())));
  }

  @Override
  public ResponseEntity<PixSettingsDTO> updatePixSettings(UpdatePixSettingsDTO dto) {
    return ResponseEntity.ok(
        toDto(
            pix.updateSettings(
                operator(),
                new SettingsUpdate(
                    dto.getEnabled(), dto.getAccessToken(), dto.getWebhookSecret()))));
  }

  @Override
  public ResponseEntity<PixChargeDTO> createPixCharge(CreateSaleDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(toDto(pix.createCharge(operator(), webMapper.toDomain(dto))));
  }

  @Override
  public ResponseEntity<PixChargeDTO> getPixCharge(String id) {
    return ResponseEntity.ok(toDto(pix.getCharge(operator(), id)));
  }

  @Override
  public ResponseEntity<PixChargeDTO> cancelPixCharge(String id) {
    return ResponseEntity.ok(toDto(pix.cancelCharge(operator(), id)));
  }

  private Operator operator() {
    return new Operator(
        userProvider.getUserId(), userProvider.getTenantId(), userProvider.getUserRole());
  }

  private PixChargeDTO toDto(PixCharge charge) {
    PixChargeDTO dto =
        new PixChargeDTO()
            .id(charge.getId())
            .status(PixChargeStatusDTO.valueOf(charge.getStatus().name()))
            .amount(charge.getAmount())
            .createdAt(time(charge.getCreatedAt()))
            .expiresAt(time(charge.getExpiresAt()))
            .paidAt(time(charge.getPaidAt()))
            .saleId(charge.getSaleId())
            .failureReason(charge.getFailureReason());
    if (charge.isPending()) {
      dto.qrCode(charge.getQrCode()).qrCodeBase64(charge.getQrCodeBase64());
    }
    if (charge.getSaleId() != null) {
      dto.sale(webMapper.toDto(sales.findById(charge.getSaleId(), charge.getTenantId())));
    }
    return dto;
  }

  private static PixSettingsDTO toDto(PixSettings settings) {
    return new PixSettingsDTO()
        .provider("MERCADO_PAGO")
        .enabled(settings.enabled())
        .configured(settings.configured())
        .accountName(settings.accountName())
        .accessTokenHint(settings.accessTokenHint())
        .webhookSecretConfigured(
            settings.webhookSecret() != null && !settings.webhookSecret().isBlank())
        .updatedAt(time(settings.updatedAt()));
  }

  private static OffsetDateTime time(LocalDateTime value) {
    return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
  }
}
