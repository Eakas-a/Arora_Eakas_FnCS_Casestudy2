package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the {@code ProductResource} paths {@link ProductEndpointTest} doesn't touch:
 * get-by-id, create (+ its validation), and update (+ its validation). Uses its own throwaway
 * products (prefixed {@code ZZ-TEST-}) rather than the seeded ones, and cleans them up
 * afterwards so it doesn't interfere with other test classes sharing the same database.
 */
@QuarkusTest
class ProductResourceCrudTest {

  private static final String PATH = "product";

  @Inject ProductRepository productRepository;

  @AfterEach
  void cleanUpProductsCreatedByThisTest() {
    QuarkusTransaction.requiringNew()
        .run(() -> productRepository.delete("name like ?1", "ZZ-TEST-%"));
  }

  @Test
  void testGetSingleProduct_found() {
    // Product id 2 ("KALLAX") is seeded and never mutated by this class.
    given().when().get(PATH + "/2").then().statusCode(200).body(containsString("KALLAX"));
  }

  @Test
  void testGetSingleProduct_notFound() {
    given().when().get(PATH + "/99999").then().statusCode(404);
  }

  @Test
  void testCreateProduct_success() {
    String newProduct = "{\"name\":\"ZZ-TEST-NEW\",\"stock\":5}";

    given()
        .contentType(ContentType.JSON)
        .body(newProduct)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("ZZ-TEST-NEW"));
  }

  @Test
  void testCreateProduct_rejectsRequestWithIdAlreadySet() {
    String invalidProduct = "{\"id\":999,\"name\":\"ZZ-TEST-INVALID\",\"stock\":1}";

    given()
        .contentType(ContentType.JSON)
        .body(invalidProduct)
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void testUpdateProduct_success() {
    Integer createdId =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"ZZ-TEST-BEFORE\",\"stock\":1}")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"ZZ-TEST-AFTER\",\"stock\":9}")
        .when()
        .put(PATH + "/" + createdId)
        .then()
        .statusCode(200)
        .body(containsString("ZZ-TEST-AFTER"));
  }

  @Test
  void testUpdateProduct_rejectsMissingName() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"stock\":1}")
        .when()
        .put(PATH + "/2")
        .then()
        .statusCode(422);
  }

  @Test
  void testUpdateProduct_notFound() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"DOES NOT MATTER\",\"stock\":1}")
        .when()
        .put(PATH + "/99999")
        .then()
        .statusCode(404);
  }

  @Test
  void testDeleteProduct_notFound() {
    given().when().delete(PATH + "/99999").then().statusCode(404);
  }
}
