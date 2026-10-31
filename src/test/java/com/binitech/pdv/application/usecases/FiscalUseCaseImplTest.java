package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.inbound.FiscalUseCasePort.ProfileCommand;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.outbound.FiscalDocumentRepositoryPort;
import com.binitech.pdv.application.ports.outbound.FiscalEmitterPort;
import com.binitech.pdv.application.ports.outbound.FiscalEmitterPort.IssuerData;
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
import com.binitech.pdv.utils.enums.FiscalDocumentStatus;
import com.binitech.pdv.utils.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FiscalUseCaseImplTest {

  private static final String TENANT = "t1";
  private static final String SALE = "s1";
  private static final String VALID_CNPJ = "11222333000181";

  @Mock private FiscalProfileRepositoryPort profileRepository;
  @Mock private FiscalDocumentRepositoryPort documentRepository;
  @Mock private FiscalEmitterPort emitter;
  @Mock private SaleUseCasePort saleUseCase;
  @Mock private ProductRepositoryPort productRepository;
  @Mock private TenantRepositoryPort tenantRepository;

  private final SecretCipher cipher =
      new SecretCipher(Base64.getEncoder().encodeToString(new byte[32]));
  private final Clock clock =
      Clock.fixed(Instant.parse("2026-10-31T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
  private FiscalUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new FiscalUseCaseImpl(
            profileRepository,
            documentRepository,
            emitter,
            saleUseCase,
            productRepository,
            tenantRepository,
            cipher,
            clock);
  }

  private ProfileCommand command(String cscToken) {
    return new ProfileCommand(
        "11.222.333/0001-81",
        "Loja Teste LTDA",
        "123.456.789.110",
        "3",
        "3534401",
        "loja@teste.com",
        "(11) 95871-0404",
        "Rua Rio Xingu",
        "400",
        null,
        "Centro",
        "Osasco",
        "sp",
        "06020-000",
        null,
        "1",
        cscToken,
        "21069090",
        null,
        null,
        true);
  }

  private FiscalProfile readyProfile() {
    FiscalProfile profile = new FiscalProfile();
    profile.setTenantId(TENANT);
    profile.setCnpj(VALID_CNPJ);
    profile.setRegimeTributario("3");
    profile.setProviderProjectId("proj-1");
    profile.setProviderApiKeyEncrypted(cipher.encrypt("ntaas_key"));
    profile.setCscConfigured(true);
    profile.setCertificateValidUntil(LocalDate.of(2027, 1, 1));
    profile.setDefaultNcm("21069090");
    profile.setEnvironment(2);
    return profile;
  }

  private Sale sale(List<Payment> payments) {
    SaleItem item = new SaleItem();
    item.setProductId("p1");
    item.setProductDescription("Refrigerante 350ml");
    item.setQuantity(2);
    item.setUnitPrice(5.0);
    item.setSubtotal(10.0);
    Sale sale = new Sale();
    sale.setId(SALE);
    sale.setTenantId(TENANT);
    sale.setItems(List.of(item));
    sale.setPayments(payments);
    sale.setTotalAmount(10.0);
    return sale;
  }

  private Payment payment(PaymentMethod method, double amount) {
    Payment payment = new Payment();
    payment.setMethod(method);
    payment.setAmount(amount);
    return payment;
  }

  @Nested
  @DisplayName("saveProfile")
  class SaveProfile {

    @Test
    @DisplayName("Cria o emitente no provedor, gera a chave e guarda a chave criptografada")
    void createsIssuerAndEncryptsKey() {
      Tenant tenant = new Tenant();
      tenant.setId(TENANT);
      tenant.setName("Loja Teste");
      when(emitter.isConfigured()).thenReturn(true);
      when(emitter.environment()).thenReturn(2);
      when(tenantRepository.findById(TENANT)).thenReturn(Optional.of(tenant));
      when(profileRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());
      when(emitter.createIssuer(any())).thenReturn("proj-1");
      when(emitter.createIssuerApiKey("proj-1")).thenReturn("ntaas_secret");
      when(profileRepository.save(any(FiscalProfile.class))).thenAnswer(returnsFirstArg());

      FiscalProfile saved = useCase.saveProfile(TENANT, command("csc-token"));

      ArgumentCaptor<IssuerData> issuer = ArgumentCaptor.forClass(IssuerData.class);
      verify(emitter).createIssuer(issuer.capture());
      assertEquals(VALID_CNPJ, issuer.getValue().cnpj());
      assertEquals("12345678911" + "0", issuer.getValue().inscricaoEstadual());
      assertEquals("SP", issuer.getValue().endereco().uf());
      assertEquals("csc-token", issuer.getValue().cscToken());
      assertEquals("proj-1", saved.getProviderProjectId());
      assertTrue(saved.isCscConfigured());
      assertEquals("ntaas_secret", cipher.decrypt(saved.getProviderApiKeyEncrypted()));
    }

    @Test
    @DisplayName("Rejeita CNPJ com dígito verificador inválido")
    void rejectsInvalidCnpj() {
      Tenant tenant = new Tenant();
      tenant.setId(TENANT);
      when(emitter.isConfigured()).thenReturn(true);
      when(tenantRepository.findById(TENANT)).thenReturn(Optional.of(tenant));
      when(profileRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());
      ProfileCommand invalid =
          new ProfileCommand(
              "11222333000180",
              "Loja",
              "1234567",
              "3",
              "3534401",
              null,
              null,
              "Rua",
              "1",
              null,
              "Centro",
              "Osasco",
              "SP",
              "06020000",
              null,
              "1",
              "token",
              null,
              null,
              null,
              false);

      assertThrows(BusinessException.class, () -> useCase.saveProfile(TENANT, invalid));
      verify(emitter, never()).createIssuer(any());
    }

    @Test
    @DisplayName("Regime normal ainda não é suportado")
    void rejectsNormalRegime() {
      Tenant tenant = new Tenant();
      tenant.setId(TENANT);
      when(emitter.isConfigured()).thenReturn(true);
      when(tenantRepository.findById(TENANT)).thenReturn(Optional.of(tenant));
      when(profileRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());
      ProfileCommand normal =
          new ProfileCommand(
              VALID_CNPJ,
              "Loja",
              "1234567",
              "1",
              "3534401",
              null,
              null,
              "Rua",
              "1",
              null,
              "Centro",
              "Osasco",
              "SP",
              "06020000",
              null,
              "1",
              "token",
              null,
              null,
              null,
              false);

      assertThrows(BusinessException.class, () -> useCase.saveProfile(TENANT, normal));
    }
  }

  @Nested
  @DisplayName("issueNfce")
  class IssueNfce {

    @Test
    @DisplayName(
        "Monta a NFC-e com NCM padrão, CFOP 5102, CSOSN 102 e desconta o troco do dinheiro")
    void buildsRequestAndQueues() {
      FiscalProfile profile = readyProfile();
      Product product = new Product();
      product.setId("p1");
      product.setTenantId(TENANT);
      product.setBarcode("7894900011517");
      when(emitter.isConfigured()).thenReturn(true);
      when(profileRepository.findByTenantId(TENANT)).thenReturn(Optional.of(profile));
      when(documentRepository.findBySaleIdAndTenantId(SALE, TENANT)).thenReturn(Optional.empty());
      when(saleUseCase.findById(SALE, TENANT))
          .thenReturn(sale(List.of(payment(PaymentMethod.CASH, 20.0))));
      when(productRepository.findById("p1")).thenReturn(Optional.of(product));
      when(emitter.issueNfce(eq("ntaas_key"), any(), eq("nfce-" + SALE))).thenReturn("inv-1");
      when(documentRepository.save(any(FiscalDocument.class))).thenAnswer(returnsFirstArg());
      when(emitter.fetchStatus("ntaas_key", "inv-1"))
          .thenReturn(
              new NfceStatus(
                  "issued",
                  7,
                  1,
                  "3526...",
                  "135",
                  100,
                  "Autorizado o uso da NF-e",
                  "https://qr",
                  "https://consulta",
                  new BigDecimal("10.00"),
                  null));

      FiscalDocument document = useCase.issueNfce(TENANT, SALE, "529.982.247-25");

      ArgumentCaptor<NfceRequest> request = ArgumentCaptor.forClass(NfceRequest.class);
      verify(emitter).issueNfce(eq("ntaas_key"), request.capture(), anyString());
      var item = request.getValue().items().get(0);
      assertEquals("21069090", item.ncm());
      assertEquals("5102", item.cfop());
      assertEquals("102", item.csosn());
      assertEquals("7894900011517", item.ean());
      assertEquals(new BigDecimal("10.00"), item.valorTotal());
      assertEquals("52998224725", request.getValue().consumer().cpf());
      assertEquals(
          List.of(new NfcePayment("01", new BigDecimal("10.00"))), request.getValue().payments());
      assertEquals(FiscalDocumentStatus.ISSUED, document.getStatus());
      assertEquals(7, document.getNumero());
      assertEquals("https://qr", document.getQrCode());
    }

    @Test
    @DisplayName("Produto sem NCM e sem NCM padrão bloqueia a emissão")
    void missingNcmFails() {
      FiscalProfile profile = readyProfile();
      profile.setDefaultNcm(null);
      when(emitter.isConfigured()).thenReturn(true);
      when(profileRepository.findByTenantId(TENANT)).thenReturn(Optional.of(profile));
      when(documentRepository.findBySaleIdAndTenantId(SALE, TENANT)).thenReturn(Optional.empty());
      when(saleUseCase.findById(SALE, TENANT)).thenReturn(sale(List.of()));
      when(productRepository.findById("p1")).thenReturn(Optional.empty());

      BusinessException error =
          assertThrows(BusinessException.class, () -> useCase.issueNfce(TENANT, SALE, null));
      assertTrue(error.getMessage().contains("NCM"));
      verify(emitter, never()).issueNfce(any(), any(), any());
    }

    @Test
    @DisplayName("Sem certificado válido não emite")
    void expiredCertificateFails() {
      FiscalProfile profile = readyProfile();
      profile.setCertificateValidUntil(LocalDate.of(2026, 1, 1));
      when(emitter.isConfigured()).thenReturn(true);
      when(profileRepository.findByTenantId(TENANT)).thenReturn(Optional.of(profile));

      assertThrows(BusinessException.class, () -> useCase.issueNfce(TENANT, SALE, null));
    }

    @Test
    @DisplayName("CPF do consumidor inválido é rejeitado")
    void invalidCpfFails() {
      when(emitter.isConfigured()).thenReturn(true);
      when(profileRepository.findByTenantId(TENANT)).thenReturn(Optional.of(readyProfile()));
      when(documentRepository.findBySaleIdAndTenantId(SALE, TENANT)).thenReturn(Optional.empty());

      assertThrows(
          BusinessException.class, () -> useCase.issueNfce(TENANT, SALE, "111.111.111-11"));
    }

    @Test
    @DisplayName("NFC-e já autorizada não é emitida de novo")
    void alreadyIssuedReturnsExisting() {
      FiscalDocument existing = new FiscalDocument();
      existing.setStatus(FiscalDocumentStatus.ISSUED);
      when(emitter.isConfigured()).thenReturn(true);
      when(profileRepository.findByTenantId(TENANT)).thenReturn(Optional.of(readyProfile()));
      when(documentRepository.findBySaleIdAndTenantId(SALE, TENANT))
          .thenReturn(Optional.of(existing));

      assertEquals(existing, useCase.issueNfce(TENANT, SALE, null));
      verify(emitter, never()).issueNfce(any(), any(), any());
    }

    @Test
    @DisplayName("Rejeição da SEFAZ fica registrada como erro com o motivo")
    void rejectionIsRecorded() {
      when(emitter.isConfigured()).thenReturn(true);
      when(profileRepository.findByTenantId(TENANT)).thenReturn(Optional.of(readyProfile()));
      when(documentRepository.findBySaleIdAndTenantId(SALE, TENANT)).thenReturn(Optional.empty());
      when(saleUseCase.findById(SALE, TENANT))
          .thenReturn(sale(List.of(payment(PaymentMethod.PIX, 10.0))));
      when(productRepository.findById("p1")).thenReturn(Optional.empty());
      when(emitter.issueNfce(any(), any(), any())).thenReturn("inv-2");
      when(documentRepository.save(any(FiscalDocument.class))).thenAnswer(returnsFirstArg());
      when(emitter.fetchStatus(any(), eq("inv-2")))
          .thenReturn(
              new NfceStatus(
                  "error",
                  null,
                  null,
                  null,
                  null,
                  778,
                  "Rejeicao: NCM inexistente",
                  null,
                  null,
                  null,
                  null));

      FiscalDocument document = useCase.issueNfce(TENANT, SALE, null);

      assertEquals(FiscalDocumentStatus.ERROR, document.getStatus());
      assertEquals(778, document.getCodigoStatus());
      assertEquals("Rejeicao: NCM inexistente", document.getMotivo());
      assertNull(document.getConsumerCpf());
    }
  }

  @Nested
  @DisplayName("payments")
  class Payments {

    @Test
    @DisplayName("Venda no crediário sem pagamento vira crédito loja (05)")
    void unpaidStoreCredit() {
      assertEquals(
          List.of(new NfcePayment("05", new BigDecimal("30.00"))),
          FiscalUseCaseImpl.payments(List.of(), new BigDecimal("30.00")));
    }

    @Test
    @DisplayName("Pagamento misto mantém cartão e Pix e tira o troco do dinheiro")
    void mixedPayments() {
      List<Payment> payments =
          List.of(payment(PaymentMethod.CREDIT_CARD, 50.0), payment(PaymentMethod.CASH, 60.0));
      assertEquals(
          List.of(
              new NfcePayment("03", new BigDecimal("50.00")),
              new NfcePayment("01", new BigDecimal("50.00"))),
          FiscalUseCaseImpl.payments(payments, new BigDecimal("100.00")));
    }
  }

  @Nested
  @DisplayName("cancelNfce")
  class CancelNfce {

    @Test
    @DisplayName("Motivo curto é rejeitado")
    void shortReasonFails() {
      when(emitter.isConfigured()).thenReturn(true);
      assertThrows(BusinessException.class, () -> useCase.cancelNfce(TENANT, SALE, "erro"));
    }

    @Test
    @DisplayName("Cancela NFC-e autorizada e acompanha até CANCELLED")
    void cancelsIssued() {
      FiscalDocument issued = new FiscalDocument();
      issued.setSaleId(SALE);
      issued.setProviderInvoiceId("inv-1");
      issued.setStatus(FiscalDocumentStatus.ISSUED);
      when(emitter.isConfigured()).thenReturn(true);
      when(profileRepository.findByTenantId(TENANT)).thenReturn(Optional.of(readyProfile()));
      when(documentRepository.findBySaleIdAndTenantId(SALE, TENANT))
          .thenReturn(Optional.of(issued));
      when(documentRepository.save(any(FiscalDocument.class))).thenAnswer(returnsFirstArg());
      when(emitter.fetchStatus("ntaas_key", "inv-1"))
          .thenReturn(
              new NfceStatus("cancelled", 7, 1, null, null, 135, null, null, null, null, null));

      FiscalDocument result =
          useCase.cancelNfce(TENANT, SALE, "Venda cancelada a pedido do cliente");

      verify(emitter).cancel("ntaas_key", "inv-1", "Venda cancelada a pedido do cliente");
      assertEquals(FiscalDocumentStatus.CANCELLED, result.getStatus());
    }
  }

  @Test
  @DisplayName("Valida CPF e CNPJ pelos dígitos verificadores")
  void documentValidation() {
    assertTrue(FiscalUseCaseImpl.validCpf("52998224725"));
    assertFalse(FiscalUseCaseImpl.validCpf("52998224724"));
    assertTrue(FiscalUseCaseImpl.validCnpj(VALID_CNPJ));
    assertTrue(FiscalUseCaseImpl.validCnpj("12ABC34501DE35"));
    assertFalse(FiscalUseCaseImpl.validCnpj("11222333000180"));
  }
}
