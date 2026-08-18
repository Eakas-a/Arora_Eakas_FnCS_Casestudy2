package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationMaxWarehousesReachedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseReplacementMismatchException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    Warehouse previousWarehouse =
        warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);

    if (previousWarehouse == null) {
      throw new WarehouseNotFoundException(newWarehouse.businessUnitCode);
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new LocationNotFoundException(newWarehouse.location);
    }

    // the stock being carried over has to physically fit in the new warehouse
    if (newWarehouse.capacity < previousWarehouse.stock) {
      throw new WarehouseCapacityExceededException(
          "New warehouse capacity of "
              + newWarehouse.capacity
              + " cannot accommodate the "
              + previousWarehouse.stock
              + " units currently stored in '"
              + previousWarehouse.businessUnitCode
              + "'.");
    }

    // the replacement is meant to be a like-for-like swap, not a chance to quietly change stock
    if (!previousWarehouse.stock.equals(newWarehouse.stock)) {
      throw new WarehouseReplacementMismatchException(
          "The new warehouse must carry over the same stock ("
              + previousWarehouse.stock
              + ") as the warehouse it is replacing.");
    }

    List<Warehouse> otherActiveWarehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(
                w ->
                    w.location.equals(newWarehouse.location)
                        && w.archivedAt == null
                        && !w.businessUnitCode.equals(previousWarehouse.businessUnitCode))
            .toList();

    if (otherActiveWarehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new LocationMaxWarehousesReachedException(
          newWarehouse.location, location.maxNumberOfWarehouses);
    }

    int capacityAlreadyUsed =
        otherActiveWarehousesAtLocation.stream().mapToInt(w -> w.capacity).sum();

    if (capacityAlreadyUsed + newWarehouse.capacity > location.maxCapacity) {
      throw new WarehouseCapacityExceededException(
          "New warehouse capacity of "
              + newWarehouse.capacity
              + " does not fit the remaining budget of location '"
              + newWarehouse.location
              + "' (max "
              + location.maxCapacity
              + ", already using "
              + capacityAlreadyUsed
              + ").");
    }

    // archive the old one first so the business unit code frees up, then bring in the new one
    previousWarehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(previousWarehouse);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }
}
