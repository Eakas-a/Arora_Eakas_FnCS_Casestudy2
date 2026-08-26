package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;

    persist(dbWarehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse dbWarehouse = findActiveByBusinessUnitCode(warehouse.businessUnitCode);

    if (dbWarehouse == null) {
      throw new IllegalStateException(
          "Cannot update a warehouse that isn't active: " + warehouse.businessUnitCode);
    }

    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    // no explicit persist() call needed here - this entity is already managed by the
    // persistence context, so Hibernate flushes the changes for us at commit time.
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse dbWarehouse = findActiveByBusinessUnitCode(warehouse.businessUnitCode);

    if (dbWarehouse != null) {
      delete(dbWarehouse);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse = findActiveByBusinessUnitCode(buCode);
    return dbWarehouse == null ? null : dbWarehouse.toWarehouse();
  }

  private DbWarehouse findActiveByBusinessUnitCode(String buCode) {
    return find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
  }
}
