package com.fulfilment.application.monolith.warehouses.domain.exceptions;

public class WarehouseCapacityExceededException extends WarehouseValidationException {

  public WarehouseCapacityExceededException(String message) {
    super(message, 400);
  }
}
