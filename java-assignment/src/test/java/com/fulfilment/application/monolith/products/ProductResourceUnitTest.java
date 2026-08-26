package com.fulfilment.application.monolith.products;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductResourceUnitTest {

  private ProductResource resource;
  private ProductRepository repository;

  @BeforeEach
  void setUp() {
    resource = new ProductResource();
    repository = mock(ProductRepository.class);
    resource.productRepository = repository;
  }

  @Test
  void getReturnsProducts() {
    Product p = new Product("P1");
    when(repository.listAll(any())).thenReturn(List.of(p));
    assertEquals(List.of(p), resource.get());
  }

  @Test
  void getSingleReturnsProduct() {
    Product p = new Product("P1");
    when(repository.findById(7L)).thenReturn(p);
    assertSame(p, resource.getSingle(7L));
  }

  @Test
  void getSingleThrowsWhenMissing() {
    when(repository.findById(7L)).thenReturn(null);
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.getSingle(7L));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void createPersistsAndReturns201() {
    Product p = new Product("P1");
    Response response = resource.create(p);
    verify(repository).persist(p);
    assertEquals(201, response.getStatus());
    assertSame(p, response.getEntity());
  }

  @Test
  void createRejectsPresetId() {
    Product p = new Product("P1");
    p.id = 9L;
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.create(p));
    assertEquals(422, ex.getResponse().getStatus());
    verifyNoInteractions(repository);
  }

  @Test
  void updateCopiesAllFields() {
    Product existing = new Product("OLD");
    Product update = new Product("NEW");
    update.description = "desc";
    update.price = new BigDecimal("12.34");
    update.stock = 8;
    when(repository.findById(5L)).thenReturn(existing);

    Product result = resource.update(5L, update);

    assertSame(existing, result);
    assertEquals("NEW", existing.name);
    assertEquals("desc", existing.description);
    assertEquals(new BigDecimal("12.34"), existing.price);
    assertEquals(8, existing.stock);
    verify(repository).persist(existing);
  }

  @Test
  void updateRejectsMissingName() {
    Product update = new Product();
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.update(5L, update));
    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void updateRejectsMissingEntity() {
    Product update = new Product("NEW");
    when(repository.findById(5L)).thenReturn(null);
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.update(5L, update));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void deleteRemovesProduct() {
    Product existing = new Product("P1");
    when(repository.findById(5L)).thenReturn(existing);
    Response response = resource.delete(5L);
    verify(repository).delete(existing);
    assertEquals(204, response.getStatus());
  }

  @Test
  void deleteRejectsMissingEntity() {
    when(repository.findById(5L)).thenReturn(null);
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.delete(5L));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void errorMapperMapsGenericAndWebExceptions() {
    ProductResource.ErrorMapper mapper = new ProductResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    Response generic = mapper.toResponse(new IllegalArgumentException("bad"));
    assertEquals(500, generic.getStatus());

    Response web = mapper.toResponse(new WebApplicationException("nope", 409));
    assertEquals(409, web.getStatus());
    assertTrue(web.getEntity().toString().contains("409"));
  }
}
