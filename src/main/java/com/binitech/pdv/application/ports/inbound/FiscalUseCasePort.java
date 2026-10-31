package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.FiscalDocument;
import com.binitech.pdv.domain.FiscalProfile;
import java.util.Optional;

public interface FiscalUseCasePort {

  Optional<FiscalProfile> getProfile(String tenantId);

  FiscalProfile saveProfile(String tenantId, ProfileCommand command);

  FiscalProfile uploadCertificate(String tenantId, byte[] certificate, String password);

  boolean isReadyToIssue(FiscalProfile profile);

  FiscalDocument issueNfce(String tenantId, String saleId, String consumerCpf);

  Optional<FiscalDocument> getNfce(String tenantId, String saleId);

  FiscalDocument cancelNfce(String tenantId, String saleId, String reason);

  byte[] downloadDanfe(String tenantId, String saleId);

  record ProfileCommand(
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
}
