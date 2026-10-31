package com.binitech.pdv.application.ports.outbound;

import java.util.Optional;

public interface AddressLookupPort {

  Optional<AddressInfo> findByCep(String cep);

  record AddressInfo(
      String cep,
      String logradouro,
      String bairro,
      String cidade,
      String uf,
      String codigoMunicipio) {}
}
