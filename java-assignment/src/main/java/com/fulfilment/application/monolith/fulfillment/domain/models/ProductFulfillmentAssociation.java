package com.fulfilment.application.monolith.fulfillment.domain.models;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;

/**
 * Domain model for product fulfillment associations.
 * Note: This is a domain model, NOT a JPA entity.
 * JPA persistence is handled by {@link com.fulfilment.application.monolith.fulfillment.adapters.database.DbProductFulfillmentAssociation}.
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
