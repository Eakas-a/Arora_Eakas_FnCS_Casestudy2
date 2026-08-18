package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationMaxWarehousesReachedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseAlreadyExistsException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;
  @Mock LocationResolver locationResolver;

  private CreateWarehouseUseCase createWarehouseUseCase;

  @BeforeEach
  public void setUp() {
    createWarehouseUseCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  private Warehouse newWarehouse(String buCode, String location, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  @Test
  public void testCreateWarehouseHappyPath() {
    var warehouse = newWarehouse("MWH.099", "ZWOLLE-002", 30, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.099")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-002")).thenReturn(new Location("ZWOLLE-002", 2, 50));
    when(warehouseStore.getAll()).thenReturn(List.of());

    createWarehouseUseCase.create(warehouse);

    verify(warehouseStore, times(1)).create(warehouse);
  }

  @Test
  public void testCreateWarehouseRejectsDuplicateBusinessUnitCode() {
    var warehouse = newWarehouse("MWH.001", "ZWOLLE-002", 30, 10);
    var existing = newWarehouse("MWH.001", "ZWOLLE-002", 30, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    assertThrows(
        WarehouseAlreadyExistsException.class, () -> createWarehouseUseCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateWarehouseRejectsUnknownLocation() {
    var warehouse = newWarehouse("MWH.099", "NARNIA-001", 30, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.099")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("NARNIA-001")).thenReturn(null);

    assertThrows(LocationNotFoundException.class, () -> createWarehouseUseCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateWarehouseRejectsWhenLocationIsFull() {
    var warehouse = newWarehouse("MWH.099", "TILBURG-001", 10, 5);
    var alreadyThere = newWarehouse("MWH.023", "TILBURG-001", 30, 27);

    when(warehouseStore.findByBusinessUnitCode("MWH.099")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("TILBURG-001")).thenReturn(new Location("TILBURG-001", 1, 40));
    when(warehouseStore.getAll()).thenReturn(List.of(alreadyThere));

    assertThrows(
        LocationMaxWarehousesReachedException.class,
        () -> createWarehouseUseCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateWarehouseRejectsWhenCapacityBudgetIsExceeded() {
    var warehouse = newWarehouse("MWH.099", "AMSTERDAM-001", 60, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.099")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.getAll()).thenReturn(List.of(newWarehouse("MWH.012", "AMSTERDAM-001", 50, 5)));

    assertThrows(
        WarehouseCapacityExceededException.class, () -> createWarehouseUseCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateWarehouseRejectsStockAboveCapacity() {
    var warehouse = newWarehouse("MWH.099", "ZWOLLE-002", 20, 25);

    when(warehouseStore.findByBusinessUnitCode("MWH.099")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-002")).thenReturn(new Location("ZWOLLE-002", 2, 50));
    when(warehouseStore.getAll()).thenReturn(List.of());

    assertThrows(
        WarehouseCapacityExceededException.class, () -> createWarehouseUseCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }
}
