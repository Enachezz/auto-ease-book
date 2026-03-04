package com.api.auto_ease.controller.car;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CarIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String toyotaMakeId;
    private String corollaModelId;
    private String camryModelId;

    private String uniqueEmail() {
        return "car-test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private String registerAndGetToken(String email, String userType) {
        var req = Map.of(
                "email", email,
                "password", "pass123",
                "fullName", "Test User",
                "userType", userType
        );
        var resp = rest.postForEntity("/api/auth/register", req, Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return (String) resp.getBody().get("token");
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @BeforeEach
    void lookupReferenceData() {
        var makesResp = rest.exchange("/api/car-makes", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        List<Map<String, Object>> makes = makesResp.getBody();

        toyotaMakeId = makes.stream()
                .filter(m -> "Toyota".equals(m.get("name")))
                .map(m -> m.get("id").toString())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Toyota not found"));

        var modelsResp = rest.exchange("/api/car-makes/" + toyotaMakeId + "/models", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        List<Map<String, Object>> models = modelsResp.getBody();

        corollaModelId = models.stream()
                .filter(m -> "Corolla".equals(m.get("name")))
                .map(m -> m.get("id").toString())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Corolla not found"));

        camryModelId = models.stream()
                .filter(m -> "Camry".equals(m.get("name")))
                .map(m -> m.get("id").toString())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Camry not found"));
    }

    // Test 1: Add car (happy path)
    @Test
    void addCarHappyPath() {
        String token = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var body = Map.of(
                "makeId", toyotaMakeId,
                "modelId", corollaModelId,
                "year", 2022,
                "color", "Blue",
                "licensePlate", "ABC-123",
                "vin", "JT2BG22K1X0123456",
                "mileage", 15000
        );

        var resp = rest.exchange("/api/cars", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(token)), Map.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Map<String, Object> car = resp.getBody();
        assertNotNull(car.get("id"));
        assertEquals("Toyota", car.get("makeName"));
        assertEquals("Corolla", car.get("modelName"));
        assertEquals(2022, car.get("year"));
        assertEquals("Blue", car.get("color"));
        assertEquals(15000, car.get("mileage"));
    }

    // Test 2: Add car — GARAGE role rejected
    @Test
    void addCarGarageRejected() {
        String token = registerAndGetToken(uniqueEmail(), "GARAGE");

        var body = Map.of(
                "makeId", toyotaMakeId,
                "modelId", corollaModelId,
                "year", 2022
        );

        var resp = rest.exchange("/api/cars", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(token)), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // Test 3: List own cars — isolation between users
    @Test
    void listOwnCarsIsolation() {
        String tokenA = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        String tokenB = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var car1 = Map.of("makeId", toyotaMakeId, "modelId", corollaModelId, "year", 2021);
        var car2 = Map.of("makeId", toyotaMakeId, "modelId", camryModelId, "year", 2023);

        rest.exchange("/api/cars", HttpMethod.POST,
                new HttpEntity<>(car1, bearerHeaders(tokenA)), Map.class);
        rest.exchange("/api/cars", HttpMethod.POST,
                new HttpEntity<>(car2, bearerHeaders(tokenA)), Map.class);

        var respA = rest.exchange("/api/cars", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenA)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertEquals(HttpStatus.OK, respA.getStatusCode());
        assertEquals(2, respA.getBody().size());

        var respB = rest.exchange("/api/cars", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenB)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertEquals(HttpStatus.OK, respB.getStatusCode());
        assertEquals(0, respB.getBody().size());
    }

    // Test 4: Update own car
    @Test
    void updateOwnCar() {
        String token = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var createBody = Map.of(
                "makeId", toyotaMakeId,
                "modelId", corollaModelId,
                "year", 2020,
                "mileage", 50000
        );
        var createResp = rest.exchange("/api/cars", HttpMethod.POST,
                new HttpEntity<>(createBody, bearerHeaders(token)), Map.class);
        Integer carId = (Integer) createResp.getBody().get("id");

        var updateBody = Map.of("mileage", 55000, "color", "Red");
        var updateResp = rest.exchange("/api/cars/" + carId, HttpMethod.PUT,
                new HttpEntity<>(updateBody, bearerHeaders(token)), Map.class);

        assertEquals(HttpStatus.OK, updateResp.getStatusCode());
        assertEquals(55000, updateResp.getBody().get("mileage"));
        assertEquals("Red", updateResp.getBody().get("color"));
        assertEquals("Corolla", updateResp.getBody().get("modelName"));
    }

    // Test 5: Update someone else's car — forbidden
    @Test
    void updateOtherUserCarForbidden() {
        String tokenA = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        String tokenB = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var createBody = Map.of("makeId", toyotaMakeId, "modelId", corollaModelId, "year", 2021);
        var createResp = rest.exchange("/api/cars", HttpMethod.POST,
                new HttpEntity<>(createBody, bearerHeaders(tokenA)), Map.class);
        Integer carId = (Integer) createResp.getBody().get("id");

        var updateBody = Map.of("mileage", 99999);
        var updateResp = rest.exchange("/api/cars/" + carId, HttpMethod.PUT,
                new HttpEntity<>(updateBody, bearerHeaders(tokenB)), String.class);

        assertEquals(HttpStatus.FORBIDDEN, updateResp.getStatusCode());
    }

    // Test 6: Delete own car
    @Test
    void deleteOwnCar() {
        String token = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var createBody = Map.of("makeId", toyotaMakeId, "modelId", corollaModelId, "year", 2019);
        var createResp = rest.exchange("/api/cars", HttpMethod.POST,
                new HttpEntity<>(createBody, bearerHeaders(token)), Map.class);
        Integer carId = (Integer) createResp.getBody().get("id");

        var deleteResp = rest.exchange("/api/cars/" + carId, HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(token)), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode());

        var listResp = rest.exchange("/api/cars", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertEquals(0, listResp.getBody().size());
    }

    // Test 7: Add car with invalid makeId — bad request
    @Test
    void addCarInvalidMakeId() {
        String token = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var body = Map.of(
                "makeId", UUID.randomUUID().toString(),
                "modelId", corollaModelId,
                "year", 2022
        );

        var resp = rest.exchange("/api/cars", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(token)), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // Test 8: Add then list — full flow with resolved names
    @Test
    void addThenListFullFlow() {
        String token = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var body = Map.of(
                "makeId", toyotaMakeId,
                "modelId", camryModelId,
                "year", 2024,
                "color", "Silver"
        );
        rest.exchange("/api/cars", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(token)), Map.class);

        var listResp = rest.exchange("/api/cars", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, listResp.getStatusCode());
        List<Map<String, Object>> cars = listResp.getBody();
        assertEquals(1, cars.size());
        assertEquals("Toyota", cars.get(0).get("makeName"));
        assertEquals("Camry", cars.get(0).get("modelName"));
        assertEquals(2024, cars.get(0).get("year"));
        assertEquals("Silver", cars.get(0).get("color"));
    }
}
