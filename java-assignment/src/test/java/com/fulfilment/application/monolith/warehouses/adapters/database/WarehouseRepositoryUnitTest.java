package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class WarehouseRepositoryUnitTest {

  private Warehouse domain() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "CODE";
    w.location = "LOC";
    w.capacity = 20;
    w.stock = 5;
    w.createdAt = LocalDateTime.now();
    return w;
  }

  private DbWarehouse db() {
    DbWarehouse d = new DbWarehouse();
    d.id = 1L;
    d.businessUnitCode = "CODE";
    d.location = "LOC";
    d.capacity = 20;
    d.stock = 5;
    d.createdAt = LocalDateTime.now();
    return d;
  }

  @Test
  void dbWarehouseMapsAllFields() {
    DbWarehouse d = db();
    d.archivedAt = LocalDateTime.now();
    Warehouse w = d.toWarehouse();
    assertEquals(d.businessUnitCode, w.businessUnitCode);
    assertEquals(d.location, w.location);
    assertEquals(d.capacity, w.capacity);
    assertEquals(d.stock, w.stock);
    assertEquals(d.createdAt, w.createdAt);
    assertEquals(d.archivedAt, w.archivedAt);
  }

  @Test
  void createAndGetAllUsePersistence() {
    WarehouseRepository repo = spy(new WarehouseRepository());
    doNothing().when(repo).persist(any(DbWarehouse.class));
    DbWarehouse d = db();
    doReturn(List.of(d)).when(repo).listAll();

    repo.create(domain());
    assertEquals(1, repo.getAll().size());
    verify(repo).persist(any(DbWarehouse.class));
  }

  @Test
  void updateFindAndRemoveCoverActiveAndMissingPaths() {
    WarehouseRepository repo = spy(new WarehouseRepository());
    PanacheQuery<DbWarehouse> query = mock(PanacheQuery.class);
    doReturn(query).when(repo).find(eq("businessUnitCode = ?1 and archivedAt is null"), eq("CODE"));
    DbWarehouse d = db();
    when(query.firstResult()).thenReturn(d);

    Warehouse w = domain();
    w.location = "NEW";
    w.capacity = 30;
    w.stock = 9;
    repo.update(w);
    assertEquals("NEW", d.location);
    assertEquals(30, d.capacity);
    assertEquals(9, d.stock);

    Warehouse found = repo.findByBusinessUnitCode("CODE");
    assertNotNull(found);
    assertEquals("CODE", found.businessUnitCode);

    doNothing().when(repo).delete(d);
    repo.remove(w);
    verify(repo).delete(d);

    when(query.firstResult()).thenReturn(null);
    assertNull(repo.findByBusinessUnitCode("CODE"));
    assertDoesNotThrow(() -> repo.remove(w));
    assertThrows(IllegalStateException.class, () -> repo.update(w));
  }
}
