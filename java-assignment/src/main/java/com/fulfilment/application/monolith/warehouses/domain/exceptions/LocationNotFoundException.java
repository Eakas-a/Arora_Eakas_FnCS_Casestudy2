package com.fulfilment.application.monolith.warehouses.domain.exceptions;

public class LocationNotFoundException extends WarehouseValidationException {

  public LocationNotFoundException(String location) {
    super("Location '" + location + "' is not a known/valid location.", 400);
  }
}
