package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream()
        .filter(warehouse -> warehouse.archivedAt == null)
        .map(this::toWarehouseResponse)
        .toList();
  }

  @Override
  @Transactional
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var warehouse = toDomainWarehouse(data);

    createWarehouseOperation.create(warehouse);

    return toWarehouseResponse(warehouseRepository.findByBusinessUnitCode(warehouse.businessUnitCode));
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    DbWarehouse dbWarehouse = findDbWarehouseOrFail(id);
    return toWarehouseResponse(dbWarehouse.toWarehouse());
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    DbWarehouse dbWarehouse = findDbWarehouseOrFail(id);
    archiveWarehouseOperation.archive(dbWarehouse.toWarehouse());
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    var newWarehouse = toDomainWarehouse(data);
    newWarehouse.businessUnitCode = businessUnitCode;

    replaceWarehouseOperation.replace(newWarehouse);

    return toWarehouseResponse(warehouseRepository.findByBusinessUnitCode(businessUnitCode));
  }

  private DbWarehouse findDbWarehouseOrFail(String id) {
    Long dbId;
    try {
      dbId = Long.valueOf(id);
    } catch (NumberFormatException e) {
      throw new WebApplicationException("Warehouse id '" + id + "' is not valid.", 400);
    }

    DbWarehouse dbWarehouse = warehouseRepository.findById(dbId);
    if (dbWarehouse == null) {
      throw new WebApplicationException("Warehouse with id of " + id + " does not exist.", 404);
    }

    return dbWarehouse;
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomainWarehouse(
      Warehouse data) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }
}
