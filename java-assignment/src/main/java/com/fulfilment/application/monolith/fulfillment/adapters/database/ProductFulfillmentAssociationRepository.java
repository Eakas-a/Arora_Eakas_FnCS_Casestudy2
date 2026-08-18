package com.fulfilment.application.monolith.fulfillment.adapters.database;

import com.fulfilment.application.monolith.fulfillment.domain.models.ProductFulfillmentAssociation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentAssociationRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ProductFulfillmentAssociationRepository
    implements FulfillmentAssociationRepository, PanacheRepository<DbProductFulfillmentAssociation> {

  @Override
  public List<ProductFulfillmentAssociation> byProductAndStore(Long productId, Long storeId) {
    return list("product.id = ?1 and store.id = ?2", productId, storeId).stream()
        .map(DbProductFulfillmentAssociation::toDomain)
        .toList();
  }

  @Override
  public List<ProductFulfillmentAssociation> byStore(Long storeId) {
    return list("store.id = ?1", storeId).stream()
        .map(DbProductFulfillmentAssociation::toDomain)
        .toList();
  }

  @Override
  public List<ProductFulfillmentAssociation> byWarehouse(Long warehouseId) {
    return list("warehouse.id = ?1", warehouseId).stream()
        .map(DbProductFulfillmentAssociation::toDomain)
        .toList();
  }

  @Override
  public ProductFulfillmentAssociation find(Long productId, Long storeId, Long warehouseId) {
    DbProductFulfillmentAssociation entity =
        find(
                "product.id = ?1 and store.id = ?2 and warehouse.id = ?3",
                productId,
                storeId,
                warehouseId)
            .firstResult();
    return entity == null ? null : entity.toDomain();
  }

  @Override
  public ProductFulfillmentAssociation create(ProductFulfillmentAssociation association) {
    DbProductFulfillmentAssociation entity = DbProductFulfillmentAssociation.fromDomain(association);
    persist(entity);
    return entity.toDomain();
  }

  @Override
  public void updateQuantity(ProductFulfillmentAssociation association) {
    DbProductFulfillmentAssociation entity = findById(association.id);
    if (entity == null) {
      throw new IllegalStateException(
          "Cannot update quantity of an association that no longer exists: " + association.id);
    }
    entity.quantity = association.quantity;
    // no explicit persist() call needed - this entity is already managed by the persistence
    // context, so Hibernate flushes the change for us at commit time.
  }
}
