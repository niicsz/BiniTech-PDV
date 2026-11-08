package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.application.ports.inbound.PixUseCasePort;
import com.binitech.pdv.application.ports.inbound.PixUseCasePort.Notification;
import com.binitech.pdv.utils.LogSanitizer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/mercadopago")
public class MercadoPagoWebhookController {

  private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);
  private static final int MAX_ID = 64;

  private final PixUseCasePort pix;

  public MercadoPagoWebhookController(PixUseCasePort pix) {
    this.pix = pix;
  }

  @PostMapping("/pix")
  public ResponseEntity<Void> handlePix(
      @RequestParam(value = "charge", required = false) String chargeId,
      @RequestParam(value = "data.id", required = false) String queryDataId,
      @RequestHeader(value = "x-signature", required = false) String signature,
      @RequestHeader(value = "x-request-id", required = false) String requestId,
      @RequestBody(required = false) Event event) {
    String dataId = queryDataId != null ? queryDataId : event == null ? null : event.dataId();
    if (!valid(chargeId) || (dataId != null && !valid(dataId))) {
      return ResponseEntity.ok().build();
    }
    try {
      pix.handleNotification(new Notification(chargeId, dataId, requestId, signature));
    } catch (RuntimeException e) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Falha ao processar notificação do Mercado Pago: cobrança={} erro={}",
            LogSanitizer.maskId(chargeId),
            e.getMessage());
      }
    }
    return ResponseEntity.ok().build();
  }

  private static boolean valid(String value) {
    return value != null
        && !value.isBlank()
        && value.length() <= MAX_ID
        && value.matches("[A-Za-z0-9-]+");
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Event(Data data) {
    String dataId() {
      return data == null ? null : data.id();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(String id) {}
}
