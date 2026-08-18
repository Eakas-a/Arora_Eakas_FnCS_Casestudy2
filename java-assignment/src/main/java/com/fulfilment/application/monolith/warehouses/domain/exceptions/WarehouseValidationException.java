package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Parent of every business-rule violation that can happen while creating, replacing or archiving
 * a Warehouse. Keeping the intended HTTP status here means the use cases stay framework agnostic
 * and the REST layer only has to translate it, not decide it.
 */
public abstract class WarehouseValidationException extends RuntimeException {

  private final int httpStatus;

  protected WarehouseValidationException(String message, int httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }

  public int getHttpStatus() {
    return httpStatus;
  }
}
