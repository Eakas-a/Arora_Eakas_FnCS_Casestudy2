package com.fulfilment.application.monolith.fulfillment.adapters.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.fulfillment.domain.models.ProductFulfillmentAssociation;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductFulfillmentAssociationRepositoryUnitTest {

  private DbProductFulfillmentAssociation entity(long id) {
    DbProductFulfillmentAssociation e = new DbProductFulfillmentAssociation();
    e.id = id;
    e.product = new Product("P");
    e.store = new Store("S");
    e.warehouse = new DbWarehouse();
    e.warehouse.id = 3L;
    e.quantity = 7;
    return e;
  }

  @Test
  void mappingMethodsAreCovered() {
    Product p = new Product("P");
    Store s = new Store("S");
    DbWarehouse w = new DbWarehouse();
    ProductFulfillmentAssociation d = new ProductFulfillmentAssociation(5L, p, s, w, 4);
    DbProductFulfillmentAssociation e = DbProductFulfillmentAssociation.fromDomain(d);
    assertSame(p, e.product);
    assertSame(s, e.store);
    assertSame(w, e.warehouse);
    assertEquals(4, e.quantity);
    ProductFulfillmentAssociation back = e.toDomain();
    assertEquals(5L, back.id);
    assertSame(p, back.product);
    assertSame(s, back.store);
    assertSame(w, back.warehouse);
  }

  @Test
  void repositoryLookupAndCreateMethodsAreCovered() {
    ProductFulfillmentAssociationRepository repo = spy(new ProductFulfillmentAssociationRepository());
    DbProductFulfillmentAssociation e = entity(1L);

    doReturn(List.of(e)).when(repo).list(eq("product.id = ?1 and store.id = ?2"), eq(1L), eq(2L));
    doReturn(List.of(e)).when(repo).list(eq("store.id = ?1"), eq(2L));
    doReturn(List.of(e)).when(repo).list(eq("warehouse.id = ?1"), eq(3L));
    PanacheQuery<DbProductFulfillmentAssociation> query = mock(PanacheQuery.class);
    doReturn(query).when(repo).find(eq("product.id = ?1 and store.id = ?2 and warehouse.id = ?3"),
        eq(1L), eq(2L), eq(3L));
    when(query.firstResult()).thenReturn(e);
    doNothing().when(repo).persist(any(DbProductFulfillmentAssociation.class));

    assertEquals(1, repo.byProductAndStore(1L, 2L).size());
    assertEquals(1, repo.byStore(2L).size());
    assertEquals(1, repo.byWarehouse(3L).size());
    assertNotNull(repo.find(1L, 2L, 3L));

    ProductFulfillmentAssociation created =
        repo.create(new ProductFulfillmentAssociation(e.product, e.store, e.warehouse, 9));
    assertEquals(9, created.quantity);
    verify(repo).persist(any(DbProductFulfillmentAssociation.class));
  }

  @Test
  void findReturnsNullAndUpdateHandlesBothPaths() {
    ProductFulfillmentAssociationRepository repo = spy(new ProductFulfillmentAssociationRepository());
    PanacheQuery<DbProductFulfillmentAssociation> query = mock(PanacheQuery.class);
    doReturn(query).when(repo).find(anyString(), any(), any(), any());
    when(query.firstResult()).thenReturn(null);
    assertNull(repo.find(1L, 2L, 3L));

    DbProductFulfillmentAssociation e = entity(10L);
    doReturn(e).when(repo).findById(10L);
    ProductFulfillmentAssociation d = e.toDomain();
    d.id = 10L;
    d.quantity = 21;
    repo.updateQuantity(d);
    assertEquals(21, e.quantity);

    doReturn(null).when(repo).findById(99L);
    ProductFulfillmentAssociation missing =
        new ProductFulfillmentAssociation();
    missing.id = 99L;
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> repo.updateQuantity(missing));
    assertTrue(ex.getMessage().contains("99"));
  }
}
