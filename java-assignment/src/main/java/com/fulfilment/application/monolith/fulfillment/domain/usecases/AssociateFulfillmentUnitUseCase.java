package com.fulfilment.application.monolith.fulfillment.domain.usecases;

import com.fulfilment.application.monolith.fulfillment.domain.exceptions.FulfillmentValidationException;
import com.fulfilment.application.monolith.fulfillment.domain.models.ProductFulfillmentAssociation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.AssociateFulfillmentUnitOperation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentAssociationRepository;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Associates a {@link DbWarehouse} as a fulfillment unit of a given {@link Product} for a given
 * {@link Store}, enforcing the bonus-task constraints:
 *
 * <ol>
 *   <li>Each Product can be fulfilled by a maximum of 2 different Warehouses per Store
 *   <li>Each Store can be fulfilled by a maximum of 3 different Warehouses
 *   <li>Each Warehouse can stock a maximum of 5 distinct types of Products
 * </ol>
 */
@ApplicationScoped
public class AssociateFulfillmentUnitUseCase implements AssociateFulfillmentUnitOperation {

  private final FulfillmentAssociationRepository associationRepository;
  private final ProductRepository productRepository;
  private final WarehouseRepository warehouseRepository;

  public AssociateFulfillmentUnitUseCase(
      FulfillmentAssociationRepository associationRepository,
      ProductRepository productRepository,
      WarehouseRepository warehouseRepository) {
    this.associationRepository = associationRepository;
    this.productRepository = productRepository;
    this.warehouseRepository = warehouseRepository;
  }

  @Override
  @Transactional
  public ProductFulfillmentAssociation associate(
      Long productId, Long storeId, Store store, Long warehouseId, int quantity) {

    Product product = productRepository.findById(productId);
    if (product == null) {
      throw new FulfillmentValidationException("Product with id " + productId + " does not exist.");
    }

    if (store == null) {
      throw new FulfillmentValidationException("Store with id " + storeId + " does not exist.");
    }

    DbWarehouse warehouse = warehouseRepository.findById(warehouseId);
    if (warehouse == null || warehouse.archivedAt != null) {
      throw new FulfillmentValidationException(
          "Warehouse with id " + warehouseId + " does not exist or is archived.");
    }

    // idempotent-ish: if this exact triple already exists, just update the quantity instead of
    // creating a duplicate row and falsely tripping the "different warehouses" limits below.
    ProductFulfillmentAssociation existing =
        associationRepository.find(productId, storeId, warehouseId);
    if (existing != null) {
      existing.quantity = quantity;
      associationRepository.updateQuantity(existing);
      return existing;
    }

    // Rule 1: a Product can be fulfilled by at most 2 different Warehouses per Store.
    List<ProductFulfillmentAssociation> productAtStore =
        associationRepository.byProductAndStore(productId, storeId);
    long distinctWarehousesForProductAtStore =
        productAtStore.stream().map(a -> a.warehouse.id).distinct().count();
    if (distinctWarehousesForProductAtStore >= 2) {
      throw new FulfillmentValidationException(
          "Product '"
              + product.name
              + "' is already fulfilled by 2 different warehouses at store '"
              + store.name
              + "'; that is the maximum allowed per store.");
    }

    // Rule 2: a Store can be fulfilled by at most 3 different Warehouses (across all products).
    List<ProductFulfillmentAssociation> atStore = associationRepository.byStore(storeId);
    long distinctWarehousesAtStore =
        atStore.stream().map(a -> a.warehouse.id).distinct().collect(Collectors.toSet()).size();
    boolean warehouseAlreadyServesStore =
        atStore.stream().anyMatch(a -> a.warehouse.id.equals(warehouseId));
    if (!warehouseAlreadyServesStore && distinctWarehousesAtStore >= 3) {
      throw new FulfillmentValidationException(
          "Store '" + store.name + "' is already fulfilled by 3 different warehouses; that is"
              + " the maximum allowed.");
    }

    // Rule 3: a Warehouse can stock at most 5 distinct types of Products (across all stores).
    List<ProductFulfillmentAssociation> atWarehouse = associationRepository.byWarehouse(warehouseId);
    long distinctProductsAtWarehouse =
        atWarehouse.stream().map(a -> a.product.id).distinct().collect(Collectors.toSet()).size();
    boolean warehouseAlreadyStocksProduct =
        atWarehouse.stream().anyMatch(a -> a.product.id.equals(productId));
    if (!warehouseAlreadyStocksProduct && distinctProductsAtWarehouse >= 5) {
      throw new FulfillmentValidationException(
          "Warehouse '"
              + warehouse.businessUnitCode
              + "' already stocks 5 different products; that is the maximum allowed.");
    }

    ProductFulfillmentAssociation association =
        new ProductFulfillmentAssociation(product, store, warehouse, quantity);
    return associationRepository.create(association);
  }
}
