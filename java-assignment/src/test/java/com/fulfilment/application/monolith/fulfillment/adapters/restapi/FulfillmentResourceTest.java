package com.fulfilment.application.monolith.fulfillment.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;

import com.fulfilment.application.monolith.fulfillment.adapters.database.ProductFulfillmentAssociationRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the {@code POST /fulfillment} endpoint end-to-end (REST layer, use case, Panache
 * repository and the {@link FulfillmentValidationExceptionMapper}), using the products/stores/
 * warehouses seeded by import.sql (ids 1-3 of each, none archived).
 *
 * <p>Each test hits the real HTTP endpoint, which commits its own transaction on a separate
 * request thread - {@code @QuarkusTest} does NOT roll that back the way it would for a plain
 * in-thread repository call. The Postgres Dev Services container is also shared across the whole
 * Maven run, not reset per test class. So we explicitly delete every association this class
 * creates after each test; otherwise leftover rows referencing e.g. product id 1 make later
 * tests (like {@code ProductEndpointTest} deleting product 1) fail with a foreign-key violation.
 */
@QuarkusTest
class FulfillmentResourceTest {

  private static final String PATH = "fulfillment";

  @Inject ProductFulfillmentAssociationRepository repository;

  @AfterEach
  void cleanUpAssociationsCreatedByThisTest() {
    QuarkusTransaction.requiringNew().run(repository::deleteAll);
  }

  private static String body(long productId, long storeId, long warehouseId, int quantity) {
    return String.format(
        "{\"productId\":%d,\"storeId\":%d,\"warehouseId\":%d,\"quantity\":%d}",
        productId, storeId, warehouseId, quantity);
  }

  @Test
  void testAssociate_createsNewAssociation() {
    given()
        .contentType(ContentType.JSON)
        .body(body(1, 1, 1, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("quantity", is(5))
        .body("product.id", is(1))
        .body("store.id", is(1))
        .body("warehouse.id", is(1));
  }

  @Test
  void testAssociate_sameTripleAgain_updatesQuantityInstead() {
    // First call creates the association.
    given()
        .contentType(ContentType.JSON)
        .body(body(2, 1, 2, 3))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("quantity", is(3));

    // Re-posting the exact same (product, store, warehouse) triple is treated as an update of
    // the existing row, not a duplicate - this exercises ProductFulfillmentAssociationRepository
    // .find(...) + .updateQuantity(...).
    given()
        .contentType(ContentType.JSON)
        .body(body(2, 1, 2, 9))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("quantity", is(9));
  }

  @Test
  void testAssociate_rejectsUnknownProduct() {
    given()
        .contentType(ContentType.JSON)
        .body(body(9999, 1, 1, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(409)
        .body("error", containsString("Product with id 9999 does not exist"));
  }

  @Test
  void testAssociate_rejectsUnknownStore() {
    given()
        .contentType(ContentType.JSON)
        .body(body(1, 9999, 1, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(409)
        .body("error", containsString("Store with id 9999 does not exist"));
  }

  @Test
  void testAssociate_rejectsUnknownWarehouse() {
    given()
        .contentType(ContentType.JSON)
        .body(body(1, 1, 9999, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(409)
        .body("error", containsString("does not exist or is archived"));
  }

  @Test
  void testAssociate_rejectsThirdWarehouseForSameProductAtSameStore() {
    // Rule: a Product can be fulfilled by at most 2 different Warehouses per Store. Only 3
    // warehouses exist in the seed data (ids 1-3), so the 2 successful calls below already use
    // all of them bar one - the 3rd distinct warehouse for the same (product, store) must fail.
    given()
        .contentType(ContentType.JSON)
        .body(body(3, 2, 1, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(body(3, 2, 2, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(body(3, 2, 3, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(409)
        .body("error", containsString("maximum allowed per store"));
  }
}
