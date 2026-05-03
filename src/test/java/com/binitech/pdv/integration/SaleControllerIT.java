package com.binitech.pdv.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.JwtTokenProvider;
import com.binitech.pdv.domain.*;
import com.binitech.pdv.utils.Enum.PaymentMethod;
import com.binitech.pdv.utils.Enum.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaleControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ProductRepositoryPort productRepository;
  @Autowired private SaleRepositoryPort saleRepository;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private MongoTemplate mongoTemplate;

  private String operatorToken;
  private String adminToken;
  private User operatorUser;
  private User adminUser;

  @BeforeEach
  void setUp() {
    mongoTemplate.getDb().getCollection("sales").drop();
    mongoTemplate.getDb().getCollection("products").drop();

    adminUser =
        userRepository
            .findByUsername("admin")
            .orElseGet(
                () -> {
                  User u = new User();
                  u.setUsername("admin");
                  u.setPassword(passwordEncoder.encode("admin123"));
                  u.setRole(Role.ADMIN);
                  return userRepository.save(u);
                });

    operatorUser =
        userRepository
            .findByUsername("sale_operator")
            .orElseGet(
                () -> {
                  User u = new User();
                  u.setUsername("sale_operator");
                  u.setPassword(passwordEncoder.encode("operator123"));
                  u.setRole(Role.OPERATOR);
                  return userRepository.save(u);
                });

    adminToken =
        jwtTokenProvider.generateAccessToken(adminUser.getId(), adminUser.getUsername(), "ADMIN");
    operatorToken =
        jwtTokenProvider.generateAccessToken(
            operatorUser.getId(), operatorUser.getUsername(), "OPERATOR");
  }

  private Product createAndSaveProduct(String barcode) {
    Product product = new Product();
    product.setBarcode(barcode);
    product.setDescription("Produto " + barcode);
    product.setPrice(10.0);
    product.setCostPrice(5.0);
    product.setStockQuantity(100);
    product.setActive(true);
    product.setUserId(operatorUser.getId());
    product.setCategory("GERAL");
    return productRepository.save(product);
  }

  private Sale createAndSaveSale(String productId) {
    Sale sale = new Sale();
    SaleItem item = new SaleItem(productId, "Produto", 1, 10.0, 5.0);
    sale.setItems(List.of(item));
    sale.setPayments(List.of(new Payment(PaymentMethod.CASH, 10.0)));
    sale.setTotalAmount(10.0);
    sale.setTotalPaid(10.0);
    sale.setTotalCost(5.0);
    sale.setChange(0.0);
    sale.setUserId(operatorUser.getId());
    sale.setTimestamp(LocalDateTime.now());
    sale.setPaid(true);
    return saleRepository.save(sale);
  }

  @Nested
  @DisplayName("POST /api/sales")
  class CreateSaleTests {

    @Test
    @DisplayName("Criar venda com dados válidos deve retornar 201")
    void createSale_withValidData_shouldReturn201() throws Exception {
      Product product = createAndSaveProduct("SALE_" + System.currentTimeMillis());

      mockMvc
          .perform(
              post("/api/sales")
                  .header("Authorization", "Bearer " + operatorToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "items",
                              List.of(Map.of("productId", product.getId(), "quantity", 2)),
                              "payments",
                              List.of(Map.of("method", "CASH", "amount", 20.0))))))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").isNotEmpty())
          .andExpect(jsonPath("$.totalAmount").value(20.0))
          .andExpect(jsonPath("$.items").isArray())
          .andExpect(jsonPath("$.items[0].productDescription").value(product.getDescription()));
    }

    @Test
    @DisplayName("Criar venda sem autenticação deve retornar 401/403")
    void createSale_withoutAuth_shouldReturn401() throws Exception {
      mockMvc
          .perform(
              post("/api/sales")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "items",
                              List.of(Map.of("productId", "fake-id", "quantity", 1)),
                              "payments",
                              List.of(Map.of("method", "CASH", "amount", 10.0))))))
          .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Criar venda com produto inexistente deve retornar 404")
    void createSale_withNonExistentProduct_shouldReturn404() throws Exception {
      mockMvc
          .perform(
              post("/api/sales")
                  .header("Authorization", "Bearer " + operatorToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "items",
                              List.of(Map.of("productId", "nonexistent-product-id", "quantity", 1)),
                              "payments",
                              List.of(Map.of("method", "CASH", "amount", 10.0))))))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Criar venda com estoque insuficiente deve retornar 400")
    void createSale_withInsufficientStock_shouldReturn400() throws Exception {
      Product product = createAndSaveProduct("LOW_STOCK_" + System.currentTimeMillis());
      product.setStockQuantity(1);
      productRepository.save(product);

      mockMvc
          .perform(
              post("/api/sales")
                  .header("Authorization", "Bearer " + operatorToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "items",
                              List.of(Map.of("productId", product.getId(), "quantity", 999)),
                              "payments",
                              List.of(Map.of("method", "CASH", "amount", 9990.0))))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    @DisplayName("Criar venda com pagamento insuficiente deve retornar 400")
    void createSale_withInsufficientPayment_shouldReturn400() throws Exception {
      Product product = createAndSaveProduct("INSUF_PAY_" + System.currentTimeMillis());

      mockMvc
          .perform(
              post("/api/sales")
                  .header("Authorization", "Bearer " + operatorToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "items",
                              List.of(Map.of("productId", product.getId(), "quantity", 5)),
                              "payments",
                              List.of(Map.of("method", "CASH", "amount", 1.0))))))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /api/sales")
  class ListSalesTests {

    @Test
    @DisplayName("Listar vendas sem filtro deve retornar 200")
    void listSales_withoutFilter_shouldReturn200() throws Exception {
      mockMvc
          .perform(get("/api/sales").header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Listar vendas por data deve retornar 200")
    void listSales_byDate_shouldReturn200() throws Exception {
      mockMvc
          .perform(
              get("/api/sales")
                  .param("date", LocalDate.now().toString())
                  .header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Listar vendas por período deve retornar 200")
    void listSales_byPeriod_shouldReturn200() throws Exception {
      mockMvc
          .perform(
              get("/api/sales")
                  .param("startDate", LocalDate.now().minusDays(7).toString())
                  .param("endDate", LocalDate.now().toString())
                  .header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray());
    }
  }

  @Nested
  @DisplayName("GET /api/sales/{id}")
  class GetSaleByIdTests {

    @Test
    @DisplayName("Buscar venda existente deve retornar 200")
    void getById_existing_shouldReturn200() throws Exception {
      Product product = createAndSaveProduct("GETSALE_" + System.currentTimeMillis());
      Sale sale = createAndSaveSale(product.getId());

      mockMvc
          .perform(
              get("/api/sales/{id}", sale.getId())
                  .header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(sale.getId()));
    }

    @Test
    @DisplayName("Buscar venda inexistente deve retornar 404")
    void getById_nonExistent_shouldReturn404() throws Exception {
      mockMvc
          .perform(
              get("/api/sales/{id}", "nonexistent-sale-id")
                  .header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("GET /api/sales/debtors")
  class ListDebtorsTests {

    @Test
    @DisplayName("Listar devedores deve retornar 200")
    void listDebtors_shouldReturn200() throws Exception {
      mockMvc
          .perform(get("/api/sales/debtors").header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray());
    }
  }

  @Nested
  @DisplayName("PATCH /api/sales/{id}/mark-paid")
  class MarkAsPaidTests {

    @Test
    @DisplayName("Marcar venda como paga deve retornar 200")
    void markAsPaid_existing_shouldReturn200() throws Exception {
      Product product = createAndSaveProduct("MARKPAID_" + System.currentTimeMillis());
      Sale sale = createAndSaveSale(product.getId());
      sale.setPaid(false);
      saleRepository.save(sale);

      mockMvc
          .perform(
              patch("/api/sales/{id}/mark-paid", sale.getId())
                  .header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.paid").value(true));
    }

    @Test
    @DisplayName("Marcar venda inexistente como paga deve retornar 404")
    void markAsPaid_nonExistent_shouldReturn404() throws Exception {
      mockMvc
          .perform(
              patch("/api/sales/{id}/mark-paid", "nonexistent-sale-id")
                  .header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isNotFound());
    }
  }
}
