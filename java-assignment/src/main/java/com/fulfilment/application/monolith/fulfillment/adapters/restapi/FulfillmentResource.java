package com.fulfilment.application.monolith.fulfillment.adapters.restapi;

import com.fulfilment.application.monolith.fulfillment.domain.models.ProductFulfillmentAssociation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.AssociateFulfillmentUnitOperation;
import com.fulfilment.application.monolith.stores.Store;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint to register a Warehouse as a fulfillment unit of a Product for a Store (bonus task).
 */
@Path("fulfillment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfillmentResource {

  @Inject AssociateFulfillmentUnitOperation associateFulfillmentUnitOperation;

  @POST
  public Response associate(FulfillmentAssociationRequest request) {
    Store store = request.storeId == null ? null : Store.findById(request.storeId);

    ProductFulfillmentAssociation association =
        associateFulfillmentUnitOperation.associate(
            request.productId, request.storeId, store, request.warehouseId, request.quantity);

    return Response.status(201).entity(association).build();
  }
}
