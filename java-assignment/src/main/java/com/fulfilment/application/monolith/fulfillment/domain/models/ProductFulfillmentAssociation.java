package com.fulfilment.application.monolith.fulfillment.domain.models;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;

/**
 * Associates a {@link Product} with a {@link Store} and the {@link DbWarehouse} that acts as its
 * fulfillment unit for that store - i.e. "this warehouse is one of the places this product is
 * shipped from, in order to stock this particular store".
 *
 * <p>A (product, store, warehouse) triple is unique: the same warehouse can't be registered twice
 * as a fulfillment unit for the same product/store pair. Quantity represents how many units of
 * the product this particular warehouse is committed to supplying to this store.
 *
 * <p>This is a framework-free domain model - persistence concerns live in the {@code adapters}
 * package, behind the {@link com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentAssociationRepository}
 * port.
 */
public class ProductFulfillmentAssociation {

  public Long id;

  public Product product;

  public Store store;

  public DbWarehouse warehouse;

  public int quantity;

  public ProductFulfillmentAssociation() {}

  public ProductFulfillmentAssociation(
      Product product, Store store, DbWarehouse warehouse, int quantity) {
    this.product = product;
    this.store = store;
    this.warehouse = warehouse;
    this.quantity = quantity;
  }

  public ProductFulfillmentAssociation(
      Long id, Product product, Store store, DbWarehouse warehouse, int quantity) {
    this.id = id;
    this.product = product;
    this.store = store;
    this.warehouse = warehouse;
    this.quantity = quantity;
  }
}
