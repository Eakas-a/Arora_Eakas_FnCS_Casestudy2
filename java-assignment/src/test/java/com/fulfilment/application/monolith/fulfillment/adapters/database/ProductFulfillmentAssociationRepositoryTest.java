package com.fulfilment.application.monolith.fulfillment.adapters.database;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.fulfillment.domain.models.ProductFulfillmentAssociation;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProductFulfillmentAssociationRepositoryTest {

  @Inject ProductFulfillmentAssociationRepository repository;
  @Inject WarehouseRepository warehouseRepository;
  @Inject ProductRepository productRepository;

  @AfterEach
  void cleanUp() {
    QuarkusTransaction.requiringNew().run(repository::deleteAll);
  }

  @Test
  void dbEntityRoundTripsToAndFromDomain() {
    Product product = productRepository.findById(1L);
    Store store = Store.findById(1L);
    DbWarehouse warehouse = new DbWarehouse();
    warehouse.id = 103L;
    warehouse.businessUnitCode = "MWH.TEST.103";

    ProductFulfillmentAssociation domain =
        new ProductFulfillmentAssociation(77L, product, store, warehouse, 9);
    DbProductFulfillmentAssociation entity = DbProductFulfillmentAssociation.fromDomain(domain);

    assertSame(product, entity.product);
    assertSame(store, entity.store);
    assertSame(warehouse, entity.warehouse);
    assertEquals(9, entity.quantity);

    entity.id = 77L;
    ProductFulfillmentAssociation roundTrip = entity.toDomain();
    assertEquals(77L, roundTrip.id);
    assertSame(product, roundTrip.product);
    assertSame(store, roundTrip.store);
    assertSame(warehouse, roundTrip.warehouse);
    assertEquals(9, roundTrip.quantity);
  }

  @Test
  void repositorySupportsCreateFindAndAllLookupMethods() {
    Product product = productRepository.findById(1L);
    Store store = Store.findById(1L);
    DbWarehouse warehouse = warehouseRepository.findById(1L);

    QuarkusTransaction.requiringNew().run(() -> {
      ProductFulfillmentAssociation created =
          repository.create(new ProductFulfillmentAssociation(product, store, warehouse, 4));
      assertNotNull(created.id);
      assertEquals(4, created.quantity);

      assertNotNull(repository.find(1L, 1L, 1L));
      assertEquals(1, repository.byProductAndStore(1L, 1L).size());
      assertEquals(1, repository.byStore(1L).size());
      assertEquals(1, repository.byWarehouse(1L).size());
    });
  }

  @Test
  void findReturnsNullWhenAssociationDoesNotExist() {
    QuarkusTransaction.requiringNew().run(() ->
        assertNull(repository.find(999991L, 999992L, 999993L)));
  }

  @Test
  void updateQuantityChangesExistingEntity() {
    Product product = productRepository.findById(2L);
    Store store = Store.findById(2L);
    DbWarehouse warehouse = warehouseRepository.findById(2L);

    QuarkusTransaction.requiringNew().run(() -> {
      ProductFulfillmentAssociation created =
          repository.create(new ProductFulfillmentAssociation(product, store, warehouse, 3));
      created.quantity = 8;
      repository.updateQuantity(created);
      assertEquals(8, repository.find(2L, 2L, 2L).quantity);
    });
  }

  @Test
  void updateQuantityRejectsMissingAssociation() {
    Product product = productRepository.findById(3L);
    Store store = Store.findById(3L);
    DbWarehouse warehouse = warehouseRepository.findById(3L);
    ProductFulfillmentAssociation missing =
        new ProductFulfillmentAssociation(product, store, warehouse, 1);
    missing.id = 999999L;

    QuarkusTransaction.requiringNew().run(() ->
        assertThrows(IllegalStateException.class, () -> repository.updateQuantity(missing)));
  }
}
