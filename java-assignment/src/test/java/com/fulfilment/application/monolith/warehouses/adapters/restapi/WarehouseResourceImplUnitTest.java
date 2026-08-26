package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WarehouseResourceImplUnitTest {

  private WarehouseResourceImpl resource;
  private WarehouseRepository repository;
  private CreateWarehouseOperation create;
  private ReplaceWarehouseOperation replace;
  private ArchiveWarehouseOperation archive;

  @BeforeEach
  void setUp() throws Exception {
    resource = new WarehouseResourceImpl();
    repository = mock(WarehouseRepository.class);
    create = mock(CreateWarehouseOperation.class);
    replace = mock(ReplaceWarehouseOperation.class);
    archive = mock(ArchiveWarehouseOperation.class);
    set("warehouseRepository", repository);
    set("createWarehouseOperation", create);
    set("replaceWarehouseOperation", replace);
    set("archiveWarehouseOperation", archive);
  }

  private void set(String field, Object value) throws Exception {
    Field f = WarehouseResourceImpl.class.getDeclaredField(field);
    f.setAccessible(true);
    f.set(resource, value);
  }

  private DbWarehouse db(Long id, String code, String location, Integer capacity, Integer stock) {
    DbWarehouse db = new DbWarehouse();
    db.id = id;
    db.businessUnitCode = code;
    db.location = location;
    db.capacity = capacity;
    db.stock = stock;
    db.createdAt = LocalDateTime.now();
    return db;
  }

  private com.warehouse.api.beans.Warehouse api(String code) {
    com.warehouse.api.beans.Warehouse w = new com.warehouse.api.beans.Warehouse();
    w.setBusinessUnitCode(code);
    w.setLocation("LOC");
    w.setCapacity(20);
    w.setStock(5);
    return w;
  }

  @Test
  void listFiltersArchivedWarehousesAndMaps() {
    DbWarehouse active = db(1L, "A", "LOC-A", 20, 5);
    DbWarehouse archived = db(2L, "B", "LOC-B", 30, 6);
    archived.archivedAt = LocalDateTime.now();
    when(repository.getAll()).thenReturn(List.of(active, archived));

    List<com.warehouse.api.beans.Warehouse> result = resource.listAllWarehousesUnits();
    assertEquals(1, result.size());
    assertEquals("A", result.get(0).getBusinessUnitCode());
  }

  @Test
  void createMapsCreatesAndReturnsCreatedWarehouse() {
    var request = api("NEW");
    DbWarehouse created = db(8L, "NEW", "LOC", 20, 5);
    WarehouseRepository repo = repository;
    when(repo.findByBusinessUnitCode("NEW")).thenReturn(created.toWarehouse());

    var response = resource.createANewWarehouseUnit(request);
    assertEquals(201, response.getStatus());
    verify(create).create(argThat(w ->
        "NEW".equals(w.businessUnitCode) && "LOC".equals(w.location)
            && Integer.valueOf(20).equals(w.capacity) && Integer.valueOf(5).equals(w.stock)));
  }

  @Test
  void getByIdHandlesFoundInvalidAndMissing() {
    when(repository.findById(1L)).thenReturn(db(1L, "A", "LOC", 10, 2));
    assertEquals("A", resource.getAWarehouseUnitByID("1").getBusinessUnitCode());

    WebApplicationExceptionAssert invalid = new WebApplicationExceptionAssert();
    invalid.assertStatus(() -> resource.getAWarehouseUnitByID("abc"), 400);

    when(repository.findById(99L)).thenReturn(null);
    invalid.assertStatus(() -> resource.getAWarehouseUnitByID("99"), 404);
  }

  @Test
  void archiveFindsAndArchivesDomainWarehouse() {
    DbWarehouse db = db(3L, "ARCH", "LOC", 10, 2);
    when(repository.findById(3L)).thenReturn(db);
    resource.archiveAWarehouseUnitByID("3");
    verify(archive).archive(argThat(w -> "ARCH".equals(w.businessUnitCode)));
  }

  @Test
  void replaceMapsAndReturnsRepositoryWarehouse() {
    var request = api("IGNORED");
    when(repository.findByBusinessUnitCode("TARGET")).thenReturn(db(4L, "TARGET", "LOC", 25, 7).toWarehouse());
    var result = resource.replaceTheCurrentActiveWarehouse("TARGET", request);
    assertEquals("TARGET", result.getBusinessUnitCode());
    verify(replace).replace(argThat(w ->
        "TARGET".equals(w.businessUnitCode)
            && "LOC".equals(w.location)
            && Integer.valueOf(20).equals(w.capacity)
            && Integer.valueOf(5).equals(w.stock)));
  }

  private static class WebApplicationExceptionAssert {
    void assertStatus(Runnable action, int status) {
      jakarta.ws.rs.WebApplicationException ex =
          assertThrows(jakarta.ws.rs.WebApplicationException.class, action::run);
      assertEquals(status, ex.getResponse().getStatus());
    }
  }
}
