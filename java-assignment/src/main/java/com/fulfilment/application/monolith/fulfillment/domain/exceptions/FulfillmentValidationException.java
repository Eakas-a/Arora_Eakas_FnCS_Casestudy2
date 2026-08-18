package com.fulfilment.application.monolith.fulfillment.domain.exceptions;

/** Raised whenever a fulfillment-unit association request breaks one of the quantity rules. */
public class FulfillmentValidationException extends RuntimeException {

  public FulfillmentValidationException(String message) {
    super(message);
  }
}
