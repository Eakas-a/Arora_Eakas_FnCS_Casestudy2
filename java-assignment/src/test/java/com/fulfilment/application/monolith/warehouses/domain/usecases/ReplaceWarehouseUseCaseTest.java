package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseReplacementMismatchException;
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
public class ReplaceWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;
  @Mock LocationResolver locationResolver;

  private ReplaceWarehouseUseCase replaceWarehouseUseCase;

  @BeforeEach
  public void setUp() {
    replaceWarehouseUseCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
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
  public void testReplaceHappyPathArchivesOldAndCreatesNewWithSameCode() {
    var previous = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
    var replacement = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(new Location("ZWOLLE-001", 1, 40));
    when(warehouseStore.getAll()).thenReturn(List.of(previous));

    replaceWarehouseUseCase.replace(replacement);

    verify(warehouseStore, times(1)).update(previous);
    verify(warehouseStore, times(1)).create(replacement);
  }

  @Test
  public void testReplaceRejectsWhenThereIsNothingToReplace() {
    var replacement = newWarehouse("MWH.999", "ZWOLLE-001", 40, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);

    assertThrows(WarehouseNotFoundException.class, () -> replaceWarehouseUseCase.replace(replacement));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testReplaceRejectsWhenNewCapacityCannotHoldPreviousStock() {
    var previous = newWarehouse("MWH.001", "ZWOLLE-001", 40, 30);
    var replacement = newWarehouse("MWH.001", "ZWOLLE-001", 20, 30);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(new Location("ZWOLLE-001", 1, 40));

    assertThrows(
        WarehouseCapacityExceededException.class,
        () -> replaceWarehouseUseCase.replace(replacement));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testReplaceRejectsWhenStockDoesNotMatchThePreviousWarehouse() {
    var previous = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
    var replacement = newWarehouse("MWH.001", "ZWOLLE-001", 40, 25);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(new Location("ZWOLLE-001", 1, 40));

    assertThrows(
        WarehouseReplacementMismatchException.class,
        () -> replaceWarehouseUseCase.replace(replacement));

    verify(warehouseStore, never()).create(any());
  }
}
