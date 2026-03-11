package com.api.auto_ease.controller.jobrequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class JobRequestAndQuoteIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String toyotaMakeId;
    private String corollaModelId;
    private String oilChangeCategoryId;

    private String uniqueEmail() {
        return "jr-test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
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
                .filter(make -> "Toyota".equals(make.get("name")))
                .map(make -> make.get("id").toString())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Toyota not found"));

        var modelsResp = rest.exchange("/api/car-makes/" + toyotaMakeId + "/models", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        corollaModelId = modelsResp.getBody().stream()
                .filter(model -> "Corolla".equals(model.get("name")))
                .map(model -> model.get("id").toString())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Corolla not found"));

        var categoriesResp = rest.exchange("/api/service-categories", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        oilChangeCategoryId = categoriesResp.getBody().stream()
                .filter(category -> "Oil Change".equals(category.get("name")))
                .map(category -> category.get("id").toString())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Oil Change category not found"));
    }

    private Map<String, Object> addCar(String token) {
        var carBody = Map.of(
                "makeId", toyotaMakeId,
                "modelId", corollaModelId,
                "year", 2022,
                "color", "Blue"
        );
        var resp = rest.exchange("/api/cars", HttpMethod.POST,
                new HttpEntity<>(carBody, bearerHeaders(token)), Map.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    private Map<String, Object> jobRequestBody(Object carId) {
        var body = new HashMap<String, Object>();
        body.put("carId", carId);
        body.put("categoryId", oilChangeCategoryId);
        body.put("title", "Schimb ulei motor");
        body.put("description", "Schimb ulei + filtru la Toyota Corolla 2022");
        body.put("urgency", "NORMAL");
        body.put("locationCity", "București");
        return body;
    }

    private void createGarageProfile(String token) {
        var garageBody = Map.of(
                "businessName", "AutoService Pro",
                "city", "București",
                "phone", "+40741000000"
        );
        rest.exchange("/api/garages", HttpMethod.POST,
                new HttpEntity<>(garageBody, bearerHeaders(token)), Map.class);
    }

    // Test 1: Create job request — happy path
    @Test
    void createJobRequestHappyPath() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);

        var resp = rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(jobRequestBody(car.get("id")), bearerHeaders(ownerToken)), Map.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body.get("id"));
        assertEquals("OPEN", body.get("status"));
        assertEquals("Schimb ulei motor", body.get("title"));
        assertEquals("Toyota", body.get("makeName"));
        assertEquals("Corolla", body.get("modelName"));
        assertEquals("Oil Change", body.get("categoryName"));
    }

    // Test 2: Create job request — car not owned by user
    @Test
    void createJobRequestCarNotOwned() {
        String ownerA = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        String ownerB = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> carA = addCar(ownerA);

        var resp = rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(jobRequestBody(carA.get("id")), bearerHeaders(ownerB)), String.class);

        assertTrue(resp.getStatusCode() == HttpStatus.FORBIDDEN || resp.getStatusCode() == HttpStatus.BAD_REQUEST);
    }

    // Test 3: Create job request — garage user rejected
    @Test
    void createJobRequestGarageRejected() {
        String garageToken = registerAndGetToken(uniqueEmail(), "GARAGE");

        var body = new HashMap<String, Object>();
        body.put("carId", 9999);
        body.put("title", "Test");

        var resp = rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(garageToken)), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // Test 4: List own job requests
    @Test
    void listOwnJobRequests() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);

        rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(jobRequestBody(car.get("id")), bearerHeaders(ownerToken)), Map.class);

        var resp = rest.exchange("/api/job-requests", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(ownerToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> jobs = resp.getBody();
        assertFalse(jobs.isEmpty());
        assertEquals("Toyota", jobs.get(0).get("makeName"));
        assertEquals("Oil Change", jobs.get(0).get("categoryName"));
    }

    // Test 5: List open job requests (garage view)
    @Test
    void listOpenJobRequestsGarageView() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);

        rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(jobRequestBody(car.get("id")), bearerHeaders(ownerToken)), Map.class);

        String garageToken = registerAndGetToken(uniqueEmail(), "GARAGE");

        var resp = rest.exchange("/api/job-requests/open", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(garageToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> jobs = resp.getBody();
        assertNotNull(jobs);
        for (Map<String, Object> job : jobs) {
            assertEquals("OPEN", job.get("status"));
        }
    }

    // Test 6: Submit quote — happy path
    @Test
    void submitQuoteHappyPath() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);

        var jobResp = rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(jobRequestBody(car.get("id")), bearerHeaders(ownerToken)), Map.class);
        String jobId = jobResp.getBody().get("id").toString();

        String garageToken = registerAndGetToken(uniqueEmail(), "GARAGE");
        createGarageProfile(garageToken);

        var quoteBody = Map.of(
                "price", 250.00,
                "estimatedDuration", "2 ore",
                "description", "Schimb ulei Castrol 5W-30 + filtru Mann",
                "warrantyInfo", "6 luni garanție"
        );
        var resp = rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.POST,
                new HttpEntity<>(quoteBody, bearerHeaders(garageToken)), Map.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Map<String, Object> quote = resp.getBody();
        assertNotNull(quote.get("id"));
        assertEquals("PENDING", quote.get("status"));
        assertEquals("AutoService Pro", quote.get("garageName"));
    }

    // Test 7: Submit quote — duplicate rejected
    @Test
    void submitQuoteDuplicateRejected() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);

        var jobResp = rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(jobRequestBody(car.get("id")), bearerHeaders(ownerToken)), Map.class);
        String jobId = jobResp.getBody().get("id").toString();

        String garageToken = registerAndGetToken(uniqueEmail(), "GARAGE");
        createGarageProfile(garageToken);

        var quoteBody = Map.of("price", 250.00, "description", "First quote");
        rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.POST,
                new HttpEntity<>(quoteBody, bearerHeaders(garageToken)), Map.class);

        var quoteBody2 = Map.of("price", 200.00, "description", "Second quote");
        var resp = rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.POST,
                new HttpEntity<>(quoteBody2, bearerHeaders(garageToken)), String.class);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    // Test 8: Submit quote — car owner rejected
    @Test
    void submitQuoteCarOwnerRejected() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);

        var jobResp = rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(jobRequestBody(car.get("id")), bearerHeaders(ownerToken)), Map.class);
        String jobId = jobResp.getBody().get("id").toString();

        var quoteBody = Map.of("price", 250.00);
        var resp = rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.POST,
                new HttpEntity<>(quoteBody, bearerHeaders(ownerToken)), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // Test 9: View quotes for own request
    @Test
    void viewQuotesForOwnRequest() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);

        var jobResp = rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(jobRequestBody(car.get("id")), bearerHeaders(ownerToken)), Map.class);
        String jobId = jobResp.getBody().get("id").toString();

        String garageToken = registerAndGetToken(uniqueEmail(), "GARAGE");
        createGarageProfile(garageToken);

        rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.POST,
                new HttpEntity<>(Map.of("price", 250.00, "description", "Quote 1"), bearerHeaders(garageToken)), Map.class);

        var resp = rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(ownerToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> quotes = resp.getBody();
        assertEquals(1, quotes.size());
        assertNotNull(quotes.get(0).get("garageName"));
        assertNotNull(quotes.get(0).get("price"));
        assertEquals("PENDING", quotes.get(0).get("status"));
    }

    // Test 10: View quotes — not your request
    @Test
    void viewQuotesNotYourRequest() {
        String ownerA = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> carA = addCar(ownerA);

        var jobResp = rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(jobRequestBody(carA.get("id")), bearerHeaders(ownerA)), Map.class);
        String jobId = jobResp.getBody().get("id").toString();

        String ownerB = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var resp = rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(ownerB)), String.class);

        assertTrue(resp.getStatusCode() == HttpStatus.FORBIDDEN || resp.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    // Test 11: Integration test — full request-quote flow
    @Test
    void ownerCreatesRequestGarageQuotes() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);

        var jobResp = rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(jobRequestBody(car.get("id")), bearerHeaders(ownerToken)), Map.class);
        assertEquals(HttpStatus.CREATED, jobResp.getStatusCode());
        String jobId = jobResp.getBody().get("id").toString();
        assertEquals("OPEN", jobResp.getBody().get("status"));

        String garageToken = registerAndGetToken(uniqueEmail(), "GARAGE");
        createGarageProfile(garageToken);

        var quoteBody = Map.of(
                "price", 100.00,
                "estimatedDuration", "1h",
                "description", "Details"
        );
        var quoteResp = rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.POST,
                new HttpEntity<>(quoteBody, bearerHeaders(garageToken)), Map.class);
        assertEquals(HttpStatus.CREATED, quoteResp.getStatusCode());
        assertEquals("PENDING", quoteResp.getBody().get("status"));

        var quotesResp = rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(ownerToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertEquals(HttpStatus.OK, quotesResp.getStatusCode());
        assertEquals(1, quotesResp.getBody().size());
        assertEquals(100.0, quotesResp.getBody().get(0).get("price"));

        var myQuotesResp = rest.exchange("/api/quotes/mine", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(garageToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertEquals(HttpStatus.OK, myQuotesResp.getStatusCode());
        assertFalse(myQuotesResp.getBody().isEmpty());
    }
}
