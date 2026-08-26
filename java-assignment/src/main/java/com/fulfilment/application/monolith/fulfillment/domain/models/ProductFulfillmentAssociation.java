package com.fulfilment.application.monolith.fulfillment.domain.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;

@Entity
public class ProductFulfillmentAssociation {

  @Id
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
