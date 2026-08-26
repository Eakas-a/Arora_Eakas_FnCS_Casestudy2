package com.fulfilment.application.monolith.warehouses.domain.exceptions;

public class WarehouseReplacementMismatchException extends WarehouseValidationException {

  public WarehouseReplacementMismatchException(String message) {
    super(message, 400);
  }
}
