package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArchiveWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;

  private ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @BeforeEach
  public void setUp() {
    archiveWarehouseUseCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  public void testArchiveSetsArchivedAtAndPersists() {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 40;
    warehouse.stock = 10;

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(warehouse);

    archiveWarehouseUseCase.archive(warehouse);

    assertNotNull(warehouse.archivedAt);
    verify(warehouseStore, times(1)).update(warehouse);
  }

  @Test
  public void testArchiveRejectsUnknownWarehouse() {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.999";

    when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);

    assertThrows(WarehouseNotFoundException.class, () -> archiveWarehouseUseCase.archive(warehouse));
  }
}
