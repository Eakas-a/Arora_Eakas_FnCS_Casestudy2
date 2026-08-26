package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.event.Event;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoreResourceUnitTest {

  @Test
  void getReturnsStores() {
    StoreResource resource = new StoreResource();
    StoreRepository repository = mock(StoreRepository.class);
    resource.storeRepository = repository;

    Store a = mock(Store.class);
    a.name = "A";
    when(repository.listAllOrderedByName()).thenReturn(List.of(a));

    assertEquals(List.of(a), resource.get());
  }

  @Test
  void getSingleFoundAndMissing() {
    StoreResource resource = new StoreResource();
    StoreRepository repository = mock(StoreRepository.class);
    resource.storeRepository = repository;

    Store a = mock(Store.class);
    a.name = "A";
    when(repository.findById(1L)).thenReturn(a);
    when(repository.findById(2L)).thenReturn(null);

    assertSame(a, resource.getSingle(1L));

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.getSingle(2L));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void createSuccessAndPresetIdValidation() {
    StoreResource resource = new StoreResource();
    StoreRepository repository = mock(StoreRepository.class);
    resource.storeRepository = repository;

    @SuppressWarnings("unchecked")
    Event<StoreChangedEvent> event = mock(Event.class);
    resource.storeChangedEvent = event;

    Store store = mock(Store.class);
    Response response = resource.create(store);
    verify(repository).persist(store);
    verify(event).fire(any(StoreChangedEvent.class));
    assertEquals(201, response.getStatus());

    Store invalid = mock(Store.class);
    invalid.id = 9L;
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.create(invalid));
    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void updateCoversValidationMissingAndSuccess() {
    StoreResource resource = new StoreResource();
    StoreRepository repository = mock(StoreRepository.class);
    resource.storeRepository = repository;

    @SuppressWarnings("unchecked")
    Event<StoreChangedEvent> event = mock(Event.class);
    resource.storeChangedEvent = event;

    Store invalid = new Store();
    when(repository.findById(anyLong())).thenReturn(null);

    WebApplicationException validation = assertThrows(WebApplicationException.class,
        () -> resource.update(1L, invalid));
    assertEquals(422, validation.getResponse().getStatus());

    Store updated = new Store("NEW");
    updated.quantityProductsInStock = 7;
    WebApplicationException missing = assertThrows(WebApplicationException.class,
        () -> resource.update(1L, updated));
    assertEquals(404, missing.getResponse().getStatus());

    // Setup for second scenario - return existing store for id 2
    Store existing = new Store("OLD");
    when(repository.findById(2L)).thenReturn(existing);
    Store result = resource.update(2L, updated);
    assertSame(existing, result);
    assertEquals("NEW", existing.name);
    assertEquals(7, existing.quantityProductsInStock);
    verify(event).fire(any(StoreChangedEvent.class));
  }

  @Test
  void patchCoversBothConditionalBranches() {
    StoreResource resource = new StoreResource();
    StoreRepository repository = mock(StoreRepository.class);
    resource.storeRepository = repository;

    @SuppressWarnings("unchecked")
    Event<StoreChangedEvent> event = mock(Event.class);
    resource.storeChangedEvent = event;

    Store existing = new Store("OLD");
    existing.quantityProductsInStock = 3;
    Store updated = new Store("NEW");
    updated.quantityProductsInStock = 9;

    // Setup for first patch - return existing store for id 1
    when(repository.findById(1L)).thenReturn(existing);

    Store result = resource.patch(1L, updated);
    assertEquals("NEW", result.name);
    assertEquals(9, result.quantityProductsInStock);

    // Setup for second patch - return nullNameZeroStock for id 2
    Store nullNameZeroStock = new Store();
    nullNameZeroStock.quantityProductsInStock = 0;
    when(repository.findById(2L)).thenReturn(nullNameZeroStock);
    Store second = resource.patch(2L, updated);
    assertNull(second.name);
    assertEquals(0, second.quantityProductsInStock);
  }

  @Test
  void patchAndDeleteValidateErrors() {
    StoreResource resource = new StoreResource();
    StoreRepository repository = mock(StoreRepository.class);
    resource.storeRepository = repository;

    @SuppressWarnings("unchecked")
    Event<StoreChangedEvent> event = mock(Event.class);
    resource.storeChangedEvent = event;

    Store invalid = new Store();
    WebApplicationException validation = assertThrows(WebApplicationException.class,
        () -> resource.patch(1L, invalid));
    assertEquals(422, validation.getResponse().getStatus());

    // Setup mock to handle different IDs differently
    when(repository.findById(1L)).thenReturn(null);
    Store storeX = new Store("X");
    when(repository.findById(2L)).thenReturn(storeX);
    when(repository.findById(3L)).thenReturn(null);

    WebApplicationException missingPatch = assertThrows(WebApplicationException.class,
        () -> resource.patch(1L, new Store("X")));
    assertEquals(404, missingPatch.getResponse().getStatus());

    assertEquals(204, resource.delete(2L).getStatus());
    verify(repository).delete(storeX);

    WebApplicationException missingDelete = assertThrows(WebApplicationException.class,
        () -> resource.delete(3L));
    assertEquals(404, missingDelete.getResponse().getStatus());
  }

  @Test
  void errorMapperMapsBothExceptionTypes() {
    StoreResource.ErrorMapper mapper = new StoreResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    assertEquals(500, mapper.toResponse(new RuntimeException("boom")).getStatus());
    assertEquals(401, mapper.toResponse(new WebApplicationException("no", 401)).getStatus());
  }
}