package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WarehouseRepositoryTest {

  @Inject WarehouseRepository repository;

  private static final String CODE = "MWH-REPO-TEST";

  @AfterEach
  void cleanUp() {
    QuarkusTransaction.requiringNew().run(() -> {
      DbWarehouse entity = repository.find("businessUnitCode = ?1", CODE).firstResult();
      if (entity != null) repository.delete(entity);
    });
  }

  private Warehouse warehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = CODE;
    warehouse.location = "HELMOND-001";
    warehouse.capacity = 20;
    warehouse.stock = 5;
    warehouse.createdAt = LocalDateTime.now();
    return warehouse;
  }

  @Test
  void dbWarehouseConvertsToDomain() {
    DbWarehouse db = new DbWarehouse();
    db.businessUnitCode = CODE;
    db.location = "HELMOND-001";
    db.capacity = 25;
    db.stock = 7;
    db.createdAt = LocalDateTime.now().minusDays(1);
    db.archivedAt = LocalDateTime.now();

    Warehouse result = db.toWarehouse();

    assertEquals(db.businessUnitCode, result.businessUnitCode);
    assertEquals(db.location, result.location);
    assertEquals(db.capacity, result.capacity);
    assertEquals(db.stock, result.stock);
    assertEquals(db.createdAt, result.createdAt);
    assertEquals(db.archivedAt, result.archivedAt);
  }

  @Test
  void createFindUpdateGetAllAndRemoveWork() {
    QuarkusTransaction.requiringNew().run(() -> {
      Warehouse warehouse = warehouse();
      repository.create(warehouse);

      Warehouse found = repository.findByBusinessUnitCode(CODE);
      assertNotNull(found);
      assertEquals(CODE, found.businessUnitCode);
      assertEquals(20, found.capacity);
      assertEquals(5, found.stock);

      warehouse.location = "EINDHOVEN-001";
      warehouse.capacity = 30;
      warehouse.stock = 8;
      repository.update(warehouse);

      Warehouse updated = repository.findByBusinessUnitCode(CODE);
      assertEquals("EINDHOVEN-001", updated.location);
      assertEquals(30, updated.capacity);
      assertEquals(8, updated.stock);
      assertFalse(repository.getAll().isEmpty());

      repository.remove(updated);
      assertNull(repository.findByBusinessUnitCode(CODE));
    });
  }

  @Test
  void findAndRemoveHandleMissingActiveWarehouse() {
    QuarkusTransaction.requiringNew().run(() -> {
      Warehouse missing = new Warehouse();
      missing.businessUnitCode = CODE;
      repository.remove(missing);
      assertNull(repository.findByBusinessUnitCode(CODE));
    });
  }

  @Test
  void updateRejectsWarehouseThatIsNotActive() {
    Warehouse missing = warehouse();
    QuarkusTransaction.requiringNew().run(() ->
        assertThrows(IllegalStateException.class, () -> repository.update(missing)));
  }
}
