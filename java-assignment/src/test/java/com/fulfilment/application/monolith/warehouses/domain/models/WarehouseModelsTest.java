package com.fulfilment.application.monolith.warehouses.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Small, framework-free domain models. Nothing here is reached by REST-level tests (they only
 * ever touch the model through its fields, never construct one via this constructor), so this
 * locks down the wiring directly.
 */
class WarehouseModelsTest {

  @Test
  void location_constructorAssignsAllFields() {
    Location location = new Location("AMSTERDAM-001", 5, 500);

    assertEquals("AMSTERDAM-001", location.identification);
    assertEquals(5, location.maxNumberOfWarehouses);
    assertEquals(500, location.maxCapacity);
  }

  @Test
  void warehouse_defaultsToNullFieldsUntilSet() {
    Warehouse warehouse = new Warehouse();

    assertNull(warehouse.businessUnitCode);
    assertNull(warehouse.location);
    assertNull(warehouse.capacity);
    assertNull(warehouse.stock);
    assertNull(warehouse.createdAt);
    assertNull(warehouse.archivedAt);

    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;

    assertEquals("MWH.001", warehouse.businessUnitCode);
    assertEquals("AMSTERDAM-001", warehouse.location);
    assertEquals(100, warehouse.capacity);
    assertEquals(50, warehouse.stock);
  }
}
