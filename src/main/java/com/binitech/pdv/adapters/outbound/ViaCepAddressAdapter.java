package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.application.ports.outbound.AddressLookupPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ViaCepAddressAdapter implements AddressLookupPort {

  private final RestClient client;

  public ViaCepAddressAdapter(RestClient.Builder restClientBuilder) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3_000);
    factory.setReadTimeout(5_000);
    this.client =
        restClientBuilder
            .baseUrl("https://viacep.com.br/ws")
            .defaultHeader(HttpHeaders.USER_AGENT, "BiniTech-PDV/1.0")
            .requestFactory(factory)
            .build();
  }

  @Override
  public Optional<AddressInfo> findByCep(String cep) {
    String digits = cep == null ? "" : cep.replaceAll("\\D", "");
    if (digits.length() != 8) {
      return Optional.empty();
    }
    try {
      ViaCepResponse response =
          client.get().uri("/{cep}/json/", digits).retrieve().body(ViaCepResponse.class);
      if (response == null || Boolean.TRUE.equals(response.erro())) {
        return Optional.empty();
      }
      return Optional.of(
          new AddressInfo(
              digits,
              response.logradouro(),
              response.bairro(),
              response.localidade(),
              response.uf(),
              response.ibge()));
    } catch (RestClientException e) {
      return Optional.empty();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ViaCepResponse(
      String logradouro, String bairro, String localidade, String uf, String ibge, Boolean erro) {}
}
