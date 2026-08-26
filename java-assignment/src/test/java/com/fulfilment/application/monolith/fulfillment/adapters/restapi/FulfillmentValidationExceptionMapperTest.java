package com.fulfilment.application.monolith.fulfillment.adapters.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.fulfillment.domain.exceptions.FulfillmentValidationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * Plain unit test (no @QuarkusTest needed) for the mapper's translation logic: a
 * FulfillmentValidationException always becomes a 409 response carrying an
 * exceptionType/code/error JSON body.
 */
class FulfillmentValidationExceptionMapperTest {

  @Test
  void toResponse_returns409WithErrorBody() {
    FulfillmentValidationExceptionMapper mapper = new FulfillmentValidationExceptionMapper();
    mapper.objectMapper = new ObjectMapper();

    FulfillmentValidationException exception =
        new FulfillmentValidationException("quantity exceeds allowed maximum");

    Response response = mapper.toResponse(exception);

    assertEquals(409, response.getStatus());

    ObjectNode body = (ObjectNode) response.getEntity();
    assertEquals(
        FulfillmentValidationException.class.getName(), body.get("exceptionType").asText());
    assertEquals(409, body.get("code").asInt());
    assertTrue(body.get("error").asText().contains("quantity exceeds allowed maximum"));
  }
}
