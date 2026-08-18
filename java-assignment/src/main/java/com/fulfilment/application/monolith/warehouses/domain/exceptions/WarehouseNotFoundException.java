package com.fulfilment.application.monolith.warehouses.domain.exceptions;

public class WarehouseNotFoundException extends WarehouseValidationException {

  public WarehouseNotFoundException(String businessUnitCode) {
    super("No active warehouse found with business unit code '" + businessUnitCode + "'.", 404);
  }
}
