package com.fulfilment.application.monolith.fulfillment.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.fulfillment.domain.exceptions.FulfillmentValidationException;
import com.fulfilment.application.monolith.fulfillment.domain.models.ProductFulfillmentAssociation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentAssociationRepository;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AssociateFulfillmentUnitUseCaseTest {

  @Mock FulfillmentAssociationRepository associationRepository;
  @Mock ProductRepository productRepository;
  @Mock WarehouseRepository warehouseRepository;

  private AssociateFulfillmentUnitUseCase useCase;

  private Store store;

  @BeforeEach
  public void setUp() {
    useCase =
        new AssociateFulfillmentUnitUseCase(
            associationRepository, productRepository, warehouseRepository);
    store = new Store("TONSTAD");
    store.id = 1L;
  }

  private Product product(long id, String name) {
    Product product = new Product(name);
    product.id = id;
    return product;
  }

  private DbWarehouse warehouse(long id, String buCode) {
    DbWarehouse warehouse = new DbWarehouse();
    warehouse.id = id;
    warehouse.businessUnitCode = buCode;
    return warehouse;
  }

  private ProductFulfillmentAssociation association(Product product, Store store, DbWarehouse warehouse) {
    return new ProductFulfillmentAssociation(product, store, warehouse, 1);
  }

  @Test
  public void testAssociateHappyPathPersistsNewAssociation() {
    Product product = product(1L, "TONSTAD");
    DbWarehouse warehouse = warehouse(10L, "MWH.001");

    when(productRepository.findById(1L)).thenReturn(product);
    when(warehouseRepository.findById(10L)).thenReturn(warehouse);
    when(associationRepository.find(1L, 1L, 10L)).thenReturn(null);
    when(associationRepository.byProductAndStore(1L, 1L)).thenReturn(List.of());
    when(associationRepository.byStore(1L)).thenReturn(List.of());
    when(associationRepository.byWarehouse(10L)).thenReturn(List.of());
    when(associationRepository.create(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ProductFulfillmentAssociation result = useCase.associate(1L, 1L, store, 10L, 5);

    assertEquals(5, result.quantity);
    assertEquals(product, result.product);
    assertEquals(warehouse, result.warehouse);
  }

  @Test
  public void testRejectsUnknownProduct() {
    when(productRepository.findById(1L)).thenReturn(null);

    assertThrows(
        FulfillmentValidationException.class, () -> useCase.associate(1L, 1L, store, 10L, 5));
  }

  @Test
  public void testRejectsArchivedWarehouse() {
    Product product = product(1L, "TONSTAD");
    DbWarehouse warehouse = warehouse(10L, "MWH.001");
    warehouse.archivedAt = java.time.LocalDateTime.now();

    when(productRepository.findById(1L)).thenReturn(product);
    when(warehouseRepository.findById(10L)).thenReturn(warehouse);

    assertThrows(
        FulfillmentValidationException.class, () -> useCase.associate(1L, 1L, store, 10L, 5));
  }

  @Test
  public void testRejectsThirdWarehouseForSameProductAtSameStore() {
    // Rule 1: a Product can be fulfilled by at most 2 different Warehouses per Store.
    Product product = product(1L, "TONSTAD");
    DbWarehouse warehouseA = warehouse(10L, "MWH.001");
    DbWarehouse warehouseB = warehouse(11L, "MWH.002");
    DbWarehouse warehouseC = warehouse(12L, "MWH.003");

    when(productRepository.findById(1L)).thenReturn(product);
    when(warehouseRepository.findById(12L)).thenReturn(warehouseC);
    when(associationRepository.find(1L, 1L, 12L)).thenReturn(null);
    when(associationRepository.byProductAndStore(1L, 1L))
        .thenReturn(
            List.of(association(product, store, warehouseA), association(product, store, warehouseB)));

    assertThrows(
        FulfillmentValidationException.class, () -> useCase.associate(1L, 1L, store, 12L, 5));
  }

  @Test
  public void testRejectsFourthWarehouseForSameStore() {
    // Rule 2: a Store can be fulfilled by at most 3 different Warehouses overall.
    Product productA = product(1L, "TONSTAD");
    Product productB = product(2L, "KALLAX");
    Product productD = product(4L, "NEW-PRODUCT");
    DbWarehouse warehouseA = warehouse(10L, "MWH.001");
    DbWarehouse warehouseB = warehouse(11L, "MWH.002");
    DbWarehouse warehouseC = warehouse(12L, "MWH.003");
    DbWarehouse warehouseD = warehouse(13L, "MWH.004");

    when(productRepository.findById(4L)).thenReturn(productD);
    when(warehouseRepository.findById(13L)).thenReturn(warehouseD);
    when(associationRepository.find(4L, 1L, 13L)).thenReturn(null);
    when(associationRepository.byProductAndStore(4L, 1L)).thenReturn(List.of());
    when(associationRepository.byStore(1L))
        .thenReturn(
            List.of(
                association(productA, store, warehouseA),
                association(productB, store, warehouseB),
                association(productA, store, warehouseC)));

    assertThrows(
        FulfillmentValidationException.class, () -> useCase.associate(4L, 1L, store, 13L, 5));
  }

  @Test
  public void testRejectsSixthProductForSameWarehouse() {
    // Rule 3: a Warehouse can stock at most 5 distinct types of Products.
    DbWarehouse warehouse = warehouse(10L, "MWH.001");
    Product newProduct = product(6L, "SIXTH-PRODUCT");

    when(productRepository.findById(6L)).thenReturn(newProduct);
    when(warehouseRepository.findById(10L)).thenReturn(warehouse);
    when(associationRepository.find(6L, 1L, 10L)).thenReturn(null);
    when(associationRepository.byProductAndStore(6L, 1L)).thenReturn(List.of());
    when(associationRepository.byStore(1L)).thenReturn(List.of());
    when(associationRepository.byWarehouse(10L))
        .thenReturn(
            List.of(
                association(product(1L, "P1"), store, warehouse),
                association(product(2L, "P2"), store, warehouse),
                association(product(3L, "P3"), store, warehouse),
                association(product(4L, "P4"), store, warehouse),
                association(product(5L, "P5"), store, warehouse)));

    assertThrows(
        FulfillmentValidationException.class, () -> useCase.associate(6L, 1L, store, 10L, 5));
  }

  @Test
  public void testExistingAssociationIsUpdatedInPlaceInsteadOfCreatingDuplicate() {
    Product product = product(1L, "TONSTAD");
    DbWarehouse warehouse = warehouse(10L, "MWH.001");
    ProductFulfillmentAssociation existing = association(product, store, warehouse);
    existing.id = 99L;

    when(productRepository.findById(1L)).thenReturn(product);
    when(warehouseRepository.findById(10L)).thenReturn(warehouse);
    when(associationRepository.find(1L, 1L, 10L)).thenReturn(existing);

    ProductFulfillmentAssociation result = useCase.associate(1L, 1L, store, 10L, 99);

    assertEquals(existing, result);
    assertEquals(99, existing.quantity);
  }
  @Test
  public void testRejectsUnknownStore() {
    Product product = product(1L, "TONSTAD");
    when(productRepository.findById(1L)).thenReturn(product);

    assertThrows(
        FulfillmentValidationException.class, () -> useCase.associate(1L, 999L, null, 10L, 5));
  }

  @Test
  public void testRejectsUnknownWarehouse() {
    Product product = product(1L, "TONSTAD");
    when(productRepository.findById(1L)).thenReturn(product);
    when(warehouseRepository.findById(10L)).thenReturn(null);

    assertThrows(
        FulfillmentValidationException.class, () -> useCase.associate(1L, 1L, store, 10L, 5));
  }

  @Test
  public void testAllowsWarehouseAlreadyServingStoreWhenStoreHasThreeWarehouses() {
    Product product = product(4L, "NEW-PRODUCT");
    DbWarehouse warehouseA = warehouse(10L, "MWH.001");
    DbWarehouse warehouseB = warehouse(11L, "MWH.002");
    DbWarehouse warehouseC = warehouse(12L, "MWH.003");

    when(productRepository.findById(4L)).thenReturn(product);
    when(warehouseRepository.findById(10L)).thenReturn(warehouseA);
    when(associationRepository.find(4L, 1L, 10L)).thenReturn(null);
    when(associationRepository.byProductAndStore(4L, 1L)).thenReturn(List.of());
    when(associationRepository.byStore(1L))
        .thenReturn(
            List.of(
                association(product(1L, "P1"), store, warehouseA),
                association(product(2L, "P2"), store, warehouseB),
                association(product(3L, "P3"), store, warehouseC)));
    when(associationRepository.byWarehouse(10L)).thenReturn(List.of());
    when(associationRepository.create(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ProductFulfillmentAssociation result = useCase.associate(4L, 1L, store, 10L, 5);

    assertEquals(warehouseA, result.warehouse);
  }

  @Test
  public void testAllowsProductAlreadyStockedByWarehouseAtFiveProductLimit() {
    Product product = product(1L, "TONSTAD");
    DbWarehouse warehouse = warehouse(10L, "MWH.001");

    when(productRepository.findById(1L)).thenReturn(product);
    when(warehouseRepository.findById(10L)).thenReturn(warehouse);
    when(associationRepository.find(1L, 1L, 10L)).thenReturn(null);
    when(associationRepository.byProductAndStore(1L, 1L)).thenReturn(List.of());
    when(associationRepository.byStore(1L)).thenReturn(List.of());
    when(associationRepository.byWarehouse(10L))
        .thenReturn(
            List.of(
                association(product, store, warehouse),
                association(product(2L, "P2"), store, warehouse),
                association(product(3L, "P3"), store, warehouse),
                association(product(4L, "P4"), store, warehouse),
                association(product(5L, "P5"), store, warehouse)));
    when(associationRepository.create(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ProductFulfillmentAssociation result = useCase.associate(1L, 1L, store, 10L, 7);

    assertEquals(7, result.quantity);
  }

}
