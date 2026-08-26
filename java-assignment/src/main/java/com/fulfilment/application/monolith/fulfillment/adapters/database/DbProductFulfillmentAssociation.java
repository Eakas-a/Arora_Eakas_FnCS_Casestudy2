package com.fulfilment.application.monolith.fulfillment.adapters.database;

import com.fulfilment.application.monolith.fulfillment.domain.models.ProductFulfillmentAssociation;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** JPA persistence model for {@link ProductFulfillmentAssociation}; stays out of the domain. */
@Entity
@Table(
    name = "product_fulfillment_association",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"product_id", "store_id", "warehouse_id"}))
public class DbProductFulfillmentAssociation extends PanacheEntity {

  @ManyToOne(optional = false)
  public Product product;

  @ManyToOne(optional = false)
  public Store store;

  @ManyToOne(optional = false)
  public DbWarehouse warehouse;

  public int quantity;

  public DbProductFulfillmentAssociation() {}

  public static DbProductFulfillmentAssociation fromDomain(ProductFulfillmentAssociation domain) {
    DbProductFulfillmentAssociation entity = new DbProductFulfillmentAssociation();
    entity.id = domain.id;
    entity.product = domain.product;
    entity.store = domain.store;
    entity.warehouse = domain.warehouse;
    entity.quantity = domain.quantity;
    return entity;
  }

  public ProductFulfillmentAssociation toDomain() {
    return new ProductFulfillmentAssociation(this.id, this.product, this.store, this.warehouse, this.quantity);
  }
}
