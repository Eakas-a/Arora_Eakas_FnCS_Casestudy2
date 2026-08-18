package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class LegacyStoreManagerGatewayTest {

  private final LegacyStoreManagerGateway gateway = new LegacyStoreManagerGateway();

  @Test
  void createStoreOnLegacySystem_doesNotThrow() {
    Store store = new Store("GATEWAY-CREATE-TEST");
    store.quantityProductsInStock = 4;

    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }

  @Test
  void updateStoreOnLegacySystem_doesNotThrow() {
    Store store = new Store("GATEWAY-UPDATE-TEST");
    store.quantityProductsInStock = 6;

    assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
  }
}
