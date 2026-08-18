package com.fulfilment.application.monolith.fulfillment.adapters.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.fulfillment.domain.exceptions.FulfillmentValidationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class FulfillmentValidationExceptionMapper
    implements ExceptionMapper<FulfillmentValidationException> {

  private static final Logger LOGGER =
      Logger.getLogger(FulfillmentValidationExceptionMapper.class.getName());

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(FulfillmentValidationException exception) {
    LOGGER.warn("Rejected fulfillment association: " + exception.getMessage());

    ObjectNode exceptionJson = objectMapper.createObjectNode();
    exceptionJson.put("exceptionType", exception.getClass().getName());
    exceptionJson.put("code", 409);
    exceptionJson.put("error", exception.getMessage());

    return Response.status(409).entity(exceptionJson).build();
  }
}
