package com.binitech.pdv.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.JwtTokenProvider;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.utils.Enum.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ProductControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ProductRepositoryPort productRepository;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private MongoTemplate mongoTemplate;

  private String adminToken;
  private String operatorToken;
  private User adminUser;
  private User operatorUser;

  @BeforeEach
  void setUp() {
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
            .findByUsername("operator_test")
            .orElseGet(
                () -> {
                  User u = new User();
                  u.setUsername("operator_test");
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

  private Product createAndSaveProduct(String barcode, String userId) {
    Product product = new Product();
    product.setBarcode(barcode);
    product.setDescription("Produto " + barcode);
    product.setPrice(10.0);
    product.setCostPrice(5.0);
    product.setStockQuantity(100);
    product.setActive(true);
    product.setUserId(userId);
    product.setCategory("GERAL");
    return productRepository.save(product);
  }

  @Nested
  @DisplayName("POST /api/products")
  class CreateProductTests {

    @Test
    @DisplayName("Criar produto com dados válidos deve retornar 201")
    void createProduct_withValidData_shouldReturn201() throws Exception {
      String uniqueBarcode = "CREATE_" + System.currentTimeMillis();
      mockMvc
          .perform(
              post("/api/products")
                  .header("Authorization", "Bearer " + operatorToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "barcode",
                              uniqueBarcode,
                              "description",
                              "Novo Produto",
                              "price",
                              15.0,
                              "costPrice",
                              8.0,
                              "stockQuantity",
                              50,
                              "category",
                              "GERAL"))))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").isNotEmpty())
          .andExpect(jsonPath("$.barcode").value(uniqueBarcode))
          .andExpect(jsonPath("$.description").value("Novo Produto"))
          .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Criar produto sem autenticação deve retornar 401/403")
    void createProduct_withoutAuth_shouldReturn401() throws Exception {
      mockMvc
          .perform(
              post("/api/products")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "barcode",
                              "UNAUTH001",
                              "description",
                              "Produto",
                              "price",
                              10.0,
                              "stockQuantity",
                              50))))
          .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Criar produto com barcode duplicado deve retornar 400")
    void createProduct_withDuplicateBarcode_shouldReturn400() throws Exception {
      String barcode = "DUP_" + System.currentTimeMillis();
      createAndSaveProduct(barcode, operatorUser.getId());

      mockMvc
          .perform(
              post("/api/products")
                  .header("Authorization", "Bearer " + operatorToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "barcode",
                              barcode,
                              "description",
                              "Duplicado",
                              "price",
                              10.0,
                              "stockQuantity",
                              50))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    @DisplayName("Criar produto sem campos obrigatórios deve retornar 400")
    void createProduct_withMissingFields_shouldReturn400() throws Exception {
      mockMvc
          .perform(
              post("/api/products")
                  .header("Authorization", "Bearer " + operatorToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("description", "Sem barcode"))))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /api/products")
  class ListProductsTests {

    @Test
    @DisplayName("Listar produtos autenticado deve retornar 200")
    void listProducts_authenticated_shouldReturn200() throws Exception {
      mockMvc
          .perform(get("/api/products").header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Listar produtos sem autenticação deve retornar 401/403")
    void listProducts_withoutAuth_shouldReturn401() throws Exception {
      mockMvc.perform(get("/api/products")).andExpect(status().is4xxClientError());
    }
  }

  @Nested
  @DisplayName("GET /api/products/{id}")
  class GetProductByIdTests {

    @Test
    @DisplayName("Buscar produto existente pelo owner deve retornar 200")
    void getById_existingProduct_shouldReturn200() throws Exception {
      Product product =
          createAndSaveProduct("GET_" + System.currentTimeMillis(), operatorUser.getId());

      mockMvc
          .perform(
              get("/api/products/{id}", product.getId())
                  .header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(product.getId()));
    }

    @Test
    @DisplayName("Buscar produto inexistente deve retornar 404")
    void getById_nonExistent_shouldReturn404() throws Exception {
      mockMvc
          .perform(
              get("/api/products/{id}", "nonexistent-id")
                  .header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("PUT /api/products/{id}")
  class UpdateProductTests {

    @Test
    @DisplayName("Atualizar produto como owner deve retornar 200")
    void updateProduct_byOwner_shouldReturn200() throws Exception {
      Product product =
          createAndSaveProduct("UPD_" + System.currentTimeMillis(), operatorUser.getId());

      mockMvc
          .perform(
              put("/api/products/{id}", product.getId())
                  .header("Authorization", "Bearer " + operatorToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "barcode",
                              product.getBarcode(),
                              "description",
                              "Atualizado",
                              "price",
                              20.0,
                              "stockQuantity",
                              75))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.description").value("Atualizado"))
          .andExpect(jsonPath("$.price").value(20.0));
    }

    @Test
    @DisplayName("Atualizar produto inexistente deve retornar 404")
    void updateProduct_nonExistent_shouldReturn404() throws Exception {
      mockMvc
          .perform(
              put("/api/products/{id}", "nonexistent-id")
                  .header("Authorization", "Bearer " + operatorToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "barcode",
                              "XXX",
                              "description",
                              "X",
                              "price",
                              10.0,
                              "stockQuantity",
                              10))))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("DELETE /api/products/{id}")
  class DeleteProductTests {

    @Test
    @DisplayName("Deletar produto como owner deve retornar 204")
    void deleteProduct_byOwner_shouldReturn204() throws Exception {
      Product product =
          createAndSaveProduct("DEL_" + System.currentTimeMillis(), operatorUser.getId());

      mockMvc
          .perform(
              delete("/api/products/{id}", product.getId())
                  .header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deletar produto inexistente deve retornar 404")
    void deleteProduct_nonExistent_shouldReturn404() throws Exception {
      mockMvc
          .perform(
              delete("/api/products/{id}", "nonexistent-id")
                  .header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("GET /api/products/barcode/{barcode}")
  class GetByBarcodeTests {

    @Test
    @DisplayName("Buscar por barcode existente deve retornar 200")
    void getByBarcode_existing_shouldReturn200() throws Exception {
      String barcode = "BAR_" + System.currentTimeMillis();
      createAndSaveProduct(barcode, operatorUser.getId());

      mockMvc
          .perform(
              get("/api/products/barcode/{barcode}", barcode)
                  .header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.barcode").value(barcode));
    }

    @Test
    @DisplayName("Buscar por barcode inexistente deve retornar 404")
    void getByBarcode_nonExistent_shouldReturn404() throws Exception {
      mockMvc
          .perform(
              get("/api/products/barcode/{barcode}", "NONEXISTENT999")
                  .header("Authorization", "Bearer " + operatorToken))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
  }
}
