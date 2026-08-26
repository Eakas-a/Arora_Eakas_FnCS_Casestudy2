package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

// Same error-shape convention as the Store/Product resources, just wired to the warehouse
// domain exceptions instead of a generic catch-all.
@Provider
public class WarehouseValidationExceptionMapper
    implements ExceptionMapper<WarehouseValidationException> {

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(WarehouseValidationException exception) {
    ObjectNode exceptionJson = objectMapper.createObjectNode();
    exceptionJson.put("exceptionType", exception.getClass().getName());
    exceptionJson.put("code", exception.getHttpStatus());
    exceptionJson.put("error", exception.getMessage());

    return Response.status(exception.getHttpStatus()).entity(exceptionJson).build();
  }
}
