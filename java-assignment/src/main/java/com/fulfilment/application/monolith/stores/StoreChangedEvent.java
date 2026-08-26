package com.fulfilment.application.monolith.stores;

/**
 * CDI event payload describing a change made to a {@link Store}.
 *
 * <p>These events are fired from within the owning JTA transaction (see {@link StoreResource}),
 * but are only observed <b>after</b> that transaction has successfully committed - see {@link
 * StoreLegacySyncObserver}. This guarantees that whatever consumes the event (today, the legacy
 * system sync) only ever reacts to a {@link Store} state that is confirmed to be persisted.
 */
public class StoreChangedEvent {

  public enum Type {
    CREATED,
    UPDATED
  }

  private final Store store;
  private final Type type;

  public StoreChangedEvent(Store store, Type type) {
    this.store = store;
    this.type = type;
  }

  public Store getStore() {
    return store;
  }

  public Type getType() {
    return type;
  }
}
