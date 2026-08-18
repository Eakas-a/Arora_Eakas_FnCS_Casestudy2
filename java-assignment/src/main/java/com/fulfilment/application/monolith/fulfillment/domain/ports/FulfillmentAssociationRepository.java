package com.fulfilment.application.monolith.fulfillment.domain.ports;

import com.fulfilment.application.monolith.fulfillment.domain.models.ProductFulfillmentAssociation;
import java.util.List;

/** Outbound port: how the domain reads and persists {@link ProductFulfillmentAssociation}s. */
public interface FulfillmentAssociationRepository {

  List<ProductFulfillmentAssociation> byProductAndStore(Long productId, Long storeId);

  List<ProductFulfillmentAssociation> byStore(Long storeId);

  List<ProductFulfillmentAssociation> byWarehouse(Long warehouseId);

  ProductFulfillmentAssociation find(Long productId, Long storeId, Long warehouseId);

  /** Creates a brand-new association and returns it with its generated id populated. */
  ProductFulfillmentAssociation create(ProductFulfillmentAssociation association);

  /** Persists a change to the quantity of an already-existing association. */
  void updateQuantity(ProductFulfillmentAssociation association);
}
