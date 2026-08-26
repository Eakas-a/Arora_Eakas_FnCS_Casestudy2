package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * These exception classes are otherwise never instantiated directly by any test, only reached
 * indirectly (if at all) through use-case wiring - so they show up as 0% covered in JaCoCo even
 * though they're tiny. This locks down each one's message and HTTP status directly.
 */
class WarehouseExceptionsTest {

  @Test
  void locationNotFoundException_buildsMessageAndStatus() {
    LocationNotFoundException ex = new LocationNotFoundException("ZWOLLE-999");

    assertTrue(ex.getMessage().contains("ZWOLLE-999"));
    assertEquals(400, ex.getHttpStatus());
  }

  @Test
  void locationMaxWarehousesReachedException_buildsMessageAndStatus() {
    LocationMaxWarehousesReachedException ex =
        new LocationMaxWarehousesReachedException("ZWOLLE-001", 2);

    assertTrue(ex.getMessage().contains("ZWOLLE-001"));
    assertTrue(ex.getMessage().contains("2"));
    assertEquals(400, ex.getHttpStatus());
  }

  @Test
  void warehouseAlreadyExistsException_buildsMessageAndStatus() {
    WarehouseAlreadyExistsException ex = new WarehouseAlreadyExistsException("MWH.001");

    assertTrue(ex.getMessage().contains("MWH.001"));
    assertEquals(400, ex.getHttpStatus());
  }

  @Test
  void warehouseCapacityExceededException_carriesMessageAndStatus() {
    WarehouseCapacityExceededException ex = new WarehouseCapacityExceededException("over capacity");

    assertEquals("over capacity", ex.getMessage());
    assertEquals(400, ex.getHttpStatus());
  }

  @Test
  void warehouseNotFoundException_buildsMessageAndStatus() {
    WarehouseNotFoundException ex = new WarehouseNotFoundException("MWH.999");

    assertTrue(ex.getMessage().contains("MWH.999"));
    assertEquals(404, ex.getHttpStatus());
  }

  @Test
  void warehouseReplacementMismatchException_carriesMessageAndStatus() {
    WarehouseReplacementMismatchException ex =
        new WarehouseReplacementMismatchException("location mismatch");

    assertEquals("location mismatch", ex.getMessage());
    assertEquals(400, ex.getHttpStatus());
  }
}
