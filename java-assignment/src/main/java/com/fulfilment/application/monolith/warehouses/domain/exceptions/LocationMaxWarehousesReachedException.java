package com.fulfilment.application.monolith.warehouses.domain.exceptions;

public class LocationMaxWarehousesReachedException extends WarehouseValidationException {

  public LocationMaxWarehousesReachedException(String location, int maxNumberOfWarehouses) {
    super(
        "Location '"
            + location
            + "' already has the maximum of "
            + maxNumberOfWarehouses
            + " warehouse(s) allowed.",
        400);
  }
}
