package com.fulfilment.application.monolith.fulfillment.domain.ports;

import com.fulfilment.application.monolith.fulfillment.domain.models.ProductFulfillmentAssociation;
import com.fulfilment.application.monolith.stores.Store;

/** Inbound port: registers a Warehouse as a fulfillment unit of a Product for a Store. */
public interface AssociateFulfillmentUnitOperation {

  ProductFulfillmentAssociation associate(
      Long productId, Long storeId, Store store, Long warehouseId, int quantity);
}
