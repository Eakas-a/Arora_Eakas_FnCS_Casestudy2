package com.fulfilment.application.monolith.warehouses.domain.exceptions;

public class WarehouseAlreadyExistsException extends WarehouseValidationException {

  public WarehouseAlreadyExistsException(String businessUnitCode) {
    super(
        "A warehouse with business unit code '" + businessUnitCode + "' already exists.", 400);
  }
}
