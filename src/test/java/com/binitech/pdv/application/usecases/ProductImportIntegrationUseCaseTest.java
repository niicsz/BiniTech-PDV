package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.binitech.pdv.application.ports.inbound.ProductImportIntegrationUseCasePort.*;
import com.binitech.pdv.application.ports.outbound.*;
import com.binitech.pdv.application.ports.outbound.ProductImportReceiptRepositoryPort.Receipt;
import com.binitech.pdv.domain.*;
import com.binitech.pdv.utils.enums.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProductImportIntegrationUseCaseTest {
  @Mock private ProductRepositoryPort products;
  @Mock private UserRepositoryPort users;
  @Mock private TenantRepositoryPort tenants;
  @Mock private ProductImportReceiptRepositoryPort receipts;

  private ProductImportIntegrationUseCase useCase;
  private ImportIdentity identity;

  @BeforeEach
  void setUp() {
    useCase = new ProductImportIntegrationUseCase(products, users, tenants, receipts);
    identity = new ImportIdentity("user-a", "tenant-a", Role.TENANT_ADMIN);
    User user = new User("user-a", "ana", "secret", Role.TENANT_ADMIN, "tenant-a");
    Tenant tenant = new Tenant();
    tenant.setId("tenant-a");
    tenant.setStatus(TenantStatus.ACTIVE);
    lenient().when(users.findByIdAndTenantId("user-a", "tenant-a")).thenReturn(Optional.of(user));
    lenient().when(tenants.findById("tenant-a")).thenReturn(Optional.of(tenant));
    lenient()
        .when(receipts.findByTenantIdAndOperationIds(anyString(), anyCollection()))
        .thenReturn(List.of());
    lenient()
        .when(products.saveAll(anyList()))
        .thenAnswer(
            invocation -> {
              List<Product> source = invocation.getArgument(0);
              for (int index = 0; index < source.size(); index++) {
                if (source.get(index).getId() == null) source.get(index).setId("saved-" + index);
              }
              return source;
            });
  }

  @Test
  void createsProductForAuthenticatedTenantAndIgnoresStockByDefault() {
    when(products.findAllByBarcodesAndTenantId(Set.of("789"), "tenant-a")).thenReturn(List.of());

    List<ImportResult> result =
        useCase.apply(
            identity,
            ImportMode.CREATE_ONLY,
            StockMode.IGNORE,
            Set.of(),
            List.of(command("job:2", 2, "789", 25)));

    assertEquals(ResultAction.CREATED, result.getFirst().action());
    var captor = org.mockito.ArgumentCaptor.forClass(List.class);
    verify(products).saveAll(captor.capture());
    Product created = (Product) captor.getValue().getFirst();
    assertEquals("tenant-a", created.getTenantId());
    assertEquals("user-a", created.getUserId());
    assertEquals(0, created.getStockQuantity());
  }

  @Test
  void replacesOrIncrementsStockOnlyWhenExplicitlyRequested() {
    Product existing = product("p-1", "789", "user-a", 10);
    when(products.findAllByBarcodesAndTenantId(Set.of("789"), "tenant-a"))
        .thenReturn(List.of(existing));

    useCase.apply(
        identity,
        ImportMode.CREATE_AND_UPDATE,
        StockMode.REPLACE,
        EnumSet.of(UpdateField.PRICE),
        List.of(command("job:2", 2, "789", 4)));
    assertEquals(4, existing.getStockQuantity());
    assertEquals(19.90, existing.getPrice());

    reset(receipts);
    when(receipts.findByTenantIdAndOperationIds(anyString(), anyCollection()))
        .thenReturn(List.of());
    useCase.apply(
        identity,
        ImportMode.CREATE_AND_UPDATE,
        StockMode.INCREMENT,
        Set.of(),
        List.of(command("job:3", 3, "789", 3)));
    assertEquals(7, existing.getStockQuantity());
  }

  @Test
  void createOnlyDoesNotOverwriteExistingProduct() {
    Product existing = product("p-1", "789", "user-a", 10);
    when(products.findAllByBarcodesAndTenantId(Set.of("789"), "tenant-a"))
        .thenReturn(List.of(existing));

    List<ImportResult> result =
        useCase.apply(
            identity,
            ImportMode.CREATE_ONLY,
            StockMode.REPLACE,
            Set.of(),
            List.of(command("job:2", 2, "789", 2)));

    assertEquals(ResultAction.IGNORED, result.getFirst().action());
    assertEquals(10, existing.getStockQuantity());
  }

  @Test
  void operatorCannotUpdateProductOwnedByAnotherUser() {
    identity = new ImportIdentity("user-a", "tenant-a", Role.OPERATOR);
    User operator = new User("user-a", "ana", "secret", Role.OPERATOR, "tenant-a");
    when(users.findByIdAndTenantId("user-a", "tenant-a")).thenReturn(Optional.of(operator));
    Product existing = product("p-1", "789", "user-b", 10);
    when(products.findAllByBarcodesAndTenantId(Set.of("789"), "tenant-a"))
        .thenReturn(List.of(existing));

    List<ImportResult> result =
        useCase.apply(
            identity,
            ImportMode.CREATE_AND_UPDATE,
            StockMode.IGNORE,
            Set.of(UpdateField.PRICE),
            List.of(command("job:2", 2, "789", 0)));

    assertEquals(ResultAction.ERROR, result.getFirst().action());
    verify(products).saveAll(List.of());
  }

  @Test
  void neverAcceptsTenantFromAUserThatDoesNotBelongToIt() {
    when(users.findByIdAndTenantId("user-a", "tenant-b")).thenReturn(Optional.empty());
    ImportIdentity forged = new ImportIdentity("user-a", "tenant-b", Role.TENANT_ADMIN);

    assertThrows(AccessDeniedException.class, () -> useCase.authorize(forged));
    verify(products, never()).saveAll(anyList());
  }

  @Test
  void reusesReceiptAndDoesNotApplySameOperationTwice() {
    Receipt prior = new Receipt("job:2", 2, "CREATED", "p-1");
    when(receipts.findByTenantIdAndOperationIds("tenant-a", Set.of("job:2")))
        .thenReturn(List.of(prior));
    when(products.findAllByBarcodesAndTenantId(Set.of("789"), "tenant-a")).thenReturn(List.of());

    List<ImportResult> result =
        useCase.apply(
            identity,
            ImportMode.CREATE_ONLY,
            StockMode.IGNORE,
            Set.of(),
            List.of(command("job:2", 2, "789", 5)));

    assertEquals(ResultAction.CREATED, result.getFirst().action());
    assertEquals("p-1", result.getFirst().productId());
    verify(products).saveAll(List.of());
    verify(receipts, never()).saveAll(anyString(), anyList());
  }

  private static ImportCommand command(
      String operationId, int line, String barcode, int stockQuantity) {
    return new ImportCommand(
        operationId,
        line,
        barcode,
        "Produto",
        new BigDecimal("19.90"),
        new BigDecimal("10.00"),
        stockQuantity,
        "Geral",
        true);
  }

  private static Product product(String id, String barcode, String ownerId, int stockQuantity) {
    Product product = new Product();
    product.setId(id);
    product.setBarcode(barcode);
    product.setDescription("Atual");
    product.setPrice(9.90);
    product.setCostPrice(5);
    product.setStockQuantity(stockQuantity);
    product.setActive(true);
    product.setCategory("Geral");
    product.setUserId(ownerId);
    product.setTenantId("tenant-a");
    return product;
  }
}
