package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationMaxWarehousesReachedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseAlreadyExistsException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WarehouseAlreadyExistsException(warehouse.businessUnitCode);
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new LocationNotFoundException(warehouse.location);
    }

    List<Warehouse> activeWarehousesAtLocation = activeWarehousesAt(warehouse.location);

    if (activeWarehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new LocationMaxWarehousesReachedException(
          warehouse.location, location.maxNumberOfWarehouses);
    }

    int capacityAlreadyUsed =
        activeWarehousesAtLocation.stream().mapToInt(w -> w.capacity).sum();

    if (capacityAlreadyUsed + warehouse.capacity > location.maxCapacity) {
      throw new WarehouseCapacityExceededException(
          "Warehouse capacity of "
              + warehouse.capacity
              + " does not fit the remaining budget of location '"
              + warehouse.location
              + "' (max "
              + location.maxCapacity
              + ", already using "
              + capacityAlreadyUsed
              + ").");
    }

    if (warehouse.stock != null && warehouse.capacity != null && warehouse.stock > warehouse.capacity) {
      throw new WarehouseCapacityExceededException(
          "Warehouse '"
              + warehouse.businessUnitCode
              + "' cannot hold a stock of "
              + warehouse.stock
              + " with only "
              + warehouse.capacity
              + " of capacity.");
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    warehouseStore.create(warehouse);
  }

  private List<Warehouse> activeWarehousesAt(String location) {
    return warehouseStore.getAll().stream()
        .filter(w -> w.location.equals(location) && w.archivedAt == null)
        .toList();
  }
}
