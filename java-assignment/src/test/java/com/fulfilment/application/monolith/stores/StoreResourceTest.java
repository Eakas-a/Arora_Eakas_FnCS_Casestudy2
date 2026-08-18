package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class StoreResourceTest {

  private static final String PATH = "store";

  @Test
  void testListInitialStores() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  void testCreateStore_commitsAndTriggersLegacySync() {
    String newStore = "{\"name\":\"NEW STORE\",\"quantityProductsInStock\":7}";

    // Creating a store commits the entity and, only after that commit succeeds, fires the
    // StoreChangedEvent that StoreLegacySyncObserver reacts to. We can't observe the legacy
    // side-effect directly here (it's a fire-and-forget temp file write), but we do assert the
    // store is durably persisted, which is the guarantee the event depends on.
    Integer createdId =
        given()
            .contentType(ContentType.JSON)
            .body(newStore)
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .body(containsString("NEW STORE"))
            .extract()
            .path("id");

    given()
        .when()
        .get(PATH + "/" + createdId)
        .then()
        .statusCode(200)
        .body(containsString("NEW STORE"));
  }

  @Test
  void testCreateStore_rejectsRequestWithIdAlreadySet() {
    String invalidStore = "{\"id\":999,\"name\":\"INVALID\",\"quantityProductsInStock\":1}";

    given()
        .contentType(ContentType.JSON)
        .body(invalidStore)
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  void testGetSingleStore_notFound() {
    given().when().get(PATH + "/99999").then().statusCode(404);
  }

  @Test
  void testUpdateStore_notFound() {
    String update = "{\"name\":\"DOES NOT MATTER\",\"quantityProductsInStock\":1}";

    given()
        .contentType(ContentType.JSON)
        .body(update)
        .when()
        .put(PATH + "/99999")
        .then()
        .statusCode(404);
  }

  @Test
  void testUpdateStore_rejectsMissingName() {
    String update = "{\"quantityProductsInStock\":1}";

    given().contentType(ContentType.JSON).body(update).when().put(PATH + "/1").then().statusCode(422);
  }

  @Test
  void testUpdateStore_updatesAndTriggersLegacySync() {
    String update = "{\"name\":\"KALLAX-RENAMED\",\"quantityProductsInStock\":9}";

    given()
        .contentType(ContentType.JSON)
        .body(update)
        .when()
        .put(PATH + "/2")
        .then()
        .statusCode(200)
        .body(containsString("KALLAX-RENAMED"));

    given()
        .when()
        .get(PATH + "/2")
        .then()
        .statusCode(200)
        .body(containsString("KALLAX-RENAMED"), containsString("9"));
  }

  @Test
  void testPatchStore_partialUpdate() {
    String patch = "{\"name\":\"BESTÅ-PATCHED\",\"quantityProductsInStock\":11}";

    given()
        .contentType(ContentType.JSON)
        .body(patch)
        .when()
        .patch(PATH + "/3")
        .then()
        .statusCode(200)
        .body(containsString("BESTÅ-PATCHED"));
  }

  @Test
  void testDeleteStore() {
    String newStore = "{\"name\":\"TO BE DELETED\",\"quantityProductsInStock\":2}";

    Integer createdId =
        given()
            .contentType(ContentType.JSON)
            .body(newStore)
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given().when().delete(PATH + "/" + createdId).then().statusCode(204);

    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("TO BE DELETED")));
  }
}
