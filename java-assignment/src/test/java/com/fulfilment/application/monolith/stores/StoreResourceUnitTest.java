package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.event.Event;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import io.quarkus.panache.common.Sort;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class StoreResourceUnitTest {

  @Test
  void getReturnsStores() {
    StoreResource resource = new StoreResource();
    Store a = mock(Store.class);
    a.name = "A";
    try (MockedStatic<Store> mocked = mockStatic(Store.class)) {
      // Use any(Sort.class) matcher for Sort parameter
      mocked.when(() -> Store.listAll(any(Sort.class))).thenReturn(List.of(a));
      assertEquals(List.of(a), resource.get());
    }
  }

  @Test
  void getSingleFoundAndMissing() {
    StoreResource resource = new StoreResource();
    Store a = mock(Store.class);
    a.name = "A";
    try (MockedStatic<Store> mocked = mockStatic(Store.class)) {
      // Use anyLong() matcher to handle any Long parameter
      mocked.when(() -> Store.findById(anyLong())).thenAnswer(invocation -> {
        Long id = invocation.getArgument(0);
        if (id == 1L) return a;
        return null;
      });
      assertSame(a, resource.getSingle(1L));

      WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.getSingle(2L));
      assertEquals(404, ex.getResponse().getStatus());
    }
  }

  @Test
  void createSuccessAndPresetIdValidation() {
    StoreResource resource = new StoreResource();
    @SuppressWarnings("unchecked")
    Event<StoreChangedEvent> event = mock(Event.class);
    resource.storeChangedEvent = event;

    Store store = mock(Store.class);
    Response response = resource.create(store);
    verify(store).persist();
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
    @SuppressWarnings("unchecked")
    Event<StoreChangedEvent> event = mock(Event.class);
    resource.storeChangedEvent = event;

    Store invalid = new Store();
    try (MockedStatic<Store> mocked = mockStatic(Store.class)) {
      // Setup for first scenario - any call returns null initially
      mocked.when(() -> Store.findById(anyLong())).thenReturn(null);
      
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
      mocked.when(() -> Store.findById(2L)).thenReturn(existing);
      Store result = resource.update(2L, updated);
      assertSame(existing, result);
      assertEquals("NEW", existing.name);
      assertEquals(7, existing.quantityProductsInStock);
      verify(event).fire(any(StoreChangedEvent.class));
    }
  }

  @Test
  void patchCoversBothConditionalBranches() {
    StoreResource resource = new StoreResource();
    @SuppressWarnings("unchecked")
    Event<StoreChangedEvent> event = mock(Event.class);
    resource.storeChangedEvent = event;

    Store existing = new Store("OLD");
    existing.quantityProductsInStock = 3;
    Store updated = new Store("NEW");
    updated.quantityProductsInStock = 9;

    try (MockedStatic<Store> mocked = mockStatic(Store.class)) {
      // Setup for first patch - return existing store for id 1
      mocked.when(() -> Store.findById(1L)).thenReturn(existing);

      Store result = resource.patch(1L, updated);
      assertEquals("NEW", result.name);
      assertEquals(9, result.quantityProductsInStock);

      // Setup for second patch - return nullNameZeroStock for id 2
      Store nullNameZeroStock = new Store();
      nullNameZeroStock.quantityProductsInStock = 0;
      mocked.when(() -> Store.findById(2L)).thenReturn(nullNameZeroStock);
      Store second = resource.patch(2L, updated);
      assertNull(second.name);
      assertEquals(0, second.quantityProductsInStock);
    }
  }

  @Test
  void patchAndDeleteValidateErrors() {
    StoreResource resource = new StoreResource();
    @SuppressWarnings("unchecked")
    Event<StoreChangedEvent> event = mock(Event.class);
    resource.storeChangedEvent = event;

    try (MockedStatic<Store> mocked = mockStatic(Store.class)) {
      Store invalid = new Store();
      WebApplicationException validation = assertThrows(WebApplicationException.class,
          () -> resource.patch(1L, invalid));
      assertEquals(422, validation.getResponse().getStatus());

      // Setup mock to handle different IDs differently
      mocked.when(() -> Store.findById(1L)).thenReturn(null);
      mocked.when(() -> Store.findById(2L)).thenReturn(new Store("X"));
      mocked.when(() -> Store.findById(3L)).thenReturn(null);

      WebApplicationException missingPatch = assertThrows(WebApplicationException.class,
          () -> resource.patch(1L, new Store("X")));
      assertEquals(404, missingPatch.getResponse().getStatus());

      assertEquals(204, resource.delete(2L).getStatus());

      WebApplicationException missingDelete = assertThrows(WebApplicationException.class,
          () -> resource.delete(3L));
      assertEquals(404, missingDelete.getResponse().getStatus());
    }
  }

  @Test
  void errorMapperMapsBothExceptionTypes() {
    StoreResource.ErrorMapper mapper = new StoreResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    assertEquals(500, mapper.toResponse(new RuntimeException("boom")).getStatus());
    assertEquals(401, mapper.toResponse(new WebApplicationException("no", 401)).getStatus());
  }
}
