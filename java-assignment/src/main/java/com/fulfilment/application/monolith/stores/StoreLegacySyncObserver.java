package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Listens for {@link StoreChangedEvent}s and forwards them to the {@link
 * LegacyStoreManagerGateway}.
 *
 * <p>The {@code during = TransactionPhase.AFTER_SUCCESS} on the observer method is the key piece:
 * CDI/Quarkus (Narayana) hooks this observer into the current JTA transaction's synchronization
 * and only invokes it once that transaction has committed successfully. If the transaction rolls
 * back for any reason, this observer is never invoked and the legacy system never hears about the
 * change - exactly the guarantee the downstream legacy system needs.
 *
 * <p>This replaces manually registering a {@code Synchronization} against the {@code
 * TransactionSynchronizationRegistry} in the resource layer: the resource simply fires a plain
 * CDI event and this observer takes care of the "only after commit" semantics, decoupling the
 * transport-layer code from the legacy-sync concern entirely.
 */
@ApplicationScoped
public class StoreLegacySyncObserver {

  private static final Logger LOGGER = Logger.getLogger(StoreLegacySyncObserver.class);

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreChanged(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreChangedEvent event) {
    Store store = event.getStore();

    switch (event.getType()) {
      case CREATED:
        LOGGER.infof("Store [%s] committed, syncing creation to legacy system", store.name);
        legacyStoreManagerGateway.createStoreOnLegacySystem(store);
        break;
      case UPDATED:
        LOGGER.infof("Store [%s] committed, syncing update to legacy system", store.name);
        legacyStoreManagerGateway.updateStoreOnLegacySystem(store);
        break;
      default:
        throw new IllegalArgumentException("Unhandled StoreChangedEvent type: " + event.getType());
    }
  }
}
