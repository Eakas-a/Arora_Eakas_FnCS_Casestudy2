package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/** Fills the small gaps left by {@link StoreResourceTest}: PATCH and DELETE error paths. */
@QuarkusTest
class StoreResourceExtraTest {

  private static final String PATH = "store";

  @Test
  void testPatchStore_rejectsMissingName() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":1}")
        .when()
        .patch(PATH + "/1")
        .then()
        .statusCode(422);
  }

  @Test
  void testPatchStore_notFound() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"DOES NOT MATTER\",\"quantityProductsInStock\":1}")
        .when()
        .patch(PATH + "/99999")
        .then()
        .statusCode(404);
  }

  @Test
  void testDeleteStore_notFound() {
    given().when().delete(PATH + "/99999").then().statusCode(404);
  }
}
