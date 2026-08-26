package com.fulfilment.application.monolith.stores;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreLegacySyncObserverTest {

  @Mock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @InjectMocks StoreLegacySyncObserver observer;

  Store store;

  @BeforeEach
  void setUp() {
    store = new Store("Downtown Store");
  }

  @Test
  void onStoreChanged_created_callsCreateOnLegacySystem() {
    observer.onStoreChanged(new StoreChangedEvent(store, StoreChangedEvent.Type.CREATED));

    verify(legacyStoreManagerGateway, times(1)).createStoreOnLegacySystem(store);
    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(store);
  }

  @Test
  void onStoreChanged_updated_callsUpdateOnLegacySystem() {
    observer.onStoreChanged(new StoreChangedEvent(store, StoreChangedEvent.Type.UPDATED));

    verify(legacyStoreManagerGateway, times(1)).updateStoreOnLegacySystem(store);
    verify(legacyStoreManagerGateway, never()).createStoreOnLegacySystem(store);
  }
}
