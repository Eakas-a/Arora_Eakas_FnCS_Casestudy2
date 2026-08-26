package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * Plain unit test (no @QuarkusTest needed) for the mapper's translation logic: any
 * WarehouseValidationException subtype becomes a JSON body carrying exceptionType/code/error,
 * with the HTTP status pulled straight from the exception itself.
 */
class WarehouseValidationExceptionMapperTest {

  @Test
  void toResponse_usesExceptionHttpStatusAndBuildsErrorBody() {
    WarehouseValidationExceptionMapper mapper = new WarehouseValidationExceptionMapper();
    mapper.objectMapper = new ObjectMapper();

    WarehouseNotFoundException exception = new WarehouseNotFoundException("MWH.999");

    Response response = mapper.toResponse(exception);

    assertEquals(404, response.getStatus());

    ObjectNode body = (ObjectNode) response.getEntity();
    assertEquals(WarehouseNotFoundException.class.getName(), body.get("exceptionType").asText());
    assertEquals(404, body.get("code").asInt());
    assertTrue(body.get("error").asText().contains("MWH.999"));
  }
}
