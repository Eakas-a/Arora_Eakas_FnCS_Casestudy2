package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@code /warehouse} end-to-end - list was already covered by {@link
 * WarehouseEndpointIT}, but create/get-by-id/archive/replace previously had no test at all (the
 * relevant assertions in {@code WarehouseEndpointIT} are commented out).
 *
 * <p>Every test that creates or archives a warehouse cleans up afterwards, since the underlying
 * Postgres Dev Services instance is shared across the whole Maven run and other test classes
 * assert on the exact seeded set of warehouses ({@code MWH.001}, {@code MWH.012}, {@code
 * MWH.023}).
 */
@QuarkusTest
class WarehouseResourceImplTest {

  private static final String PATH = "warehouse";
  private static final Set<String> SEEDED_CODES = Set.of("MWH.001", "MWH.012", "MWH.023");

  @Inject WarehouseRepository warehouseRepository;

  @AfterEach
  void cleanUpWarehousesCreatedByThisTest() {
    QuarkusTransaction.requiringNew()
        .run(
            () ->
                warehouseRepository.listAll().stream()
                    .filter(w -> !SEEDED_CODES.contains(w.businessUnitCode))
                    .forEach(warehouseRepository::delete));
  }

  private Long idOf(String businessUnitCode) {
    return QuarkusTransaction.requiringNew()
        .call(() -> warehouseRepository.find("businessUnitCode = ?1", businessUnitCode)
            .firstResult()
            .id);
  }

  private static String body(String businessUnitCode, String location, int capacity, int stock) {
    return String.format(
        "{\"businessUnitCode\":\"%s\",\"location\":\"%s\",\"capacity\":%d,\"stock\":%d}",
        businessUnitCode, location, capacity, stock);
  }

  @Test
  void testCreateWarehouse_success() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.101", "HELMOND-001", 20, 10))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("MWH.101"), containsString("HELMOND-001"));
  }

  @Test
  void testCreateWarehouse_rejectsDuplicateBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.001", "AMSTERDAM-001", 10, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("already exists"));
  }

  @Test
  void testCreateWarehouse_rejectsUnknownLocation() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.102", "NOWHERE-001", 10, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("not a known/valid location"));
  }

  @Test
  void testCreateWarehouse_rejectsWhenLocationBudgetExceeded() {
    // HELMOND-001's maxCapacity is 45 (see LocationGateway) and nothing is created there by any
    // other test - a capacity of 999 blows straight through the remaining budget.
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.103", "HELMOND-001", 999, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("does not fit the remaining budget"));
  }

  @Test
  void testCreateWarehouse_rejectsWhenStockExceedsCapacity() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.104", "VETSBY-001", 10, 50))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("cannot hold a stock of"));
  }

  @Test
  void testCreateWarehouse_rejectsWhenLocationMaxWarehousesReached() {
    // HELMOND-001 allows only 1 warehouse (see LocationGateway).
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.105", "HELMOND-001", 20, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.106", "HELMOND-001", 5, 2))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("maximum of 1 warehouse"));
  }

  @Test
  void testGetWarehouseById_found() {
    given().when().get(PATH + "/1").then().statusCode(200).body(containsString("MWH.001"));
  }

  @Test
  void testGetWarehouseById_notFound() {
    given().when().get(PATH + "/99999").then().statusCode(404);
  }

  @Test
  void testGetWarehouseById_rejectsNonNumericId() {
    given().when().get(PATH + "/not-a-number").then().statusCode(400);
  }

  @Test
  void testArchiveWarehouse_removesItFromTheListing() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.107", "EINDHOVEN-001", 15, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    Long id = idOf("MWH.107");

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given().when().get(PATH).then().statusCode(200).body(not(containsString("MWH.107")));
  }

  @Test
  void testArchiveWarehouse_notFound() {
    given().when().delete(PATH + "/99999").then().statusCode(404);
  }

  @Test
  void testReplaceWarehouse_success() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.108", "EINDHOVEN-001", 20, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(body("ignored-in-favor-of-path-param", "EINDHOVEN-001", 25, 5))
        .when()
        .post(PATH + "/MWH.108/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.108"));
  }

  @Test
  void testReplaceWarehouse_notFound() {
    given()
        .contentType(ContentType.JSON)
        .body(body("ignored", "EINDHOVEN-001", 25, 5))
        .when()
        .post(PATH + "/MWH-DOES-NOT-EXIST/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  void testReplaceWarehouse_rejectsStockMismatch() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.109", "VETSBY-001", 10, 3))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(body("ignored", "VETSBY-001", 20, 99))
        .when()
        .post(PATH + "/MWH.109/replacement")
        .then()
        .statusCode(400)
        .body("error", containsString("must carry over the same stock"));
  }
}
