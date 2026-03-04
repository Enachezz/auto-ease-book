package com.api.auto_ease.controller.garage;

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
class GarageIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String uniqueEmail() {
        return "garage-test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
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

    private Map<String, Object> garageBody() {
        return Map.of(
                "businessName", "AutoService Pro",
                "address", "Str. Republicii 10",
                "city", "Cluj-Napoca",
                "state", "Cluj",
                "postalCode", "400001",
                "phone", "+40741000000",
                "description", "Full service auto repair",
                "services", List.of("Oil Change", "Brake Service", "Engine Repair")
        );
    }

    // Test 1: Create garage — happy path
    @Test
    void createGarageHappyPath() {
        String token = registerAndGetToken(uniqueEmail(), "GARAGE");

        var resp = rest.exchange("/api/garages", HttpMethod.POST,
                new HttpEntity<>(garageBody(), bearerHeaders(token)), Map.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body.get("id"));
        assertEquals("AutoService Pro", body.get("businessName"));
        assertEquals("Cluj-Napoca", body.get("city"));
        assertEquals(false, body.get("isApproved"));
        assertNotNull(body.get("services"));
    }

    // Test 2: Create garage — car owner rejected
    @Test
    void createGarageCarOwnerRejected() {
        String token = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var resp = rest.exchange("/api/garages", HttpMethod.POST,
                new HttpEntity<>(garageBody(), bearerHeaders(token)), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // Test 3: Create garage — duplicate rejected
    @Test
    void createGarageDuplicateRejected() {
        String token = registerAndGetToken(uniqueEmail(), "GARAGE");

        rest.exchange("/api/garages", HttpMethod.POST,
                new HttpEntity<>(garageBody(), bearerHeaders(token)), Map.class);

        var resp = rest.exchange("/api/garages", HttpMethod.POST,
                new HttpEntity<>(garageBody(), bearerHeaders(token)), String.class);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    // Test 4: Get own garage
    @Test
    void getOwnGarage() {
        String token = registerAndGetToken(uniqueEmail(), "GARAGE");

        rest.exchange("/api/garages", HttpMethod.POST,
                new HttpEntity<>(garageBody(), bearerHeaders(token)), Map.class);

        var resp = rest.exchange("/api/garages/me", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)), Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("AutoService Pro", resp.getBody().get("businessName"));
        assertEquals("Full service auto repair", resp.getBody().get("description"));
    }

    // Test 5: Update own garage
    @Test
    void updateOwnGarage() {
        String token = registerAndGetToken(uniqueEmail(), "GARAGE");

        rest.exchange("/api/garages", HttpMethod.POST,
                new HttpEntity<>(garageBody(), bearerHeaders(token)), Map.class);

        var updateBody = Map.of(
                "description", "Updated description",
                "services", List.of("Oil Change", "Tire Service")
        );
        var updateResp = rest.exchange("/api/garages/me", HttpMethod.PUT,
                new HttpEntity<>(updateBody, bearerHeaders(token)), Map.class);

        assertEquals(HttpStatus.OK, updateResp.getStatusCode());

        var getResp = rest.exchange("/api/garages/me", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)), Map.class);

        assertEquals("Updated description", getResp.getBody().get("description"));
        assertEquals("AutoService Pro", getResp.getBody().get("businessName"));
    }

    // Test 6: List approved garages — public (none approved => empty)
    @Test
    void listApprovedGaragesPublic() {
        String token = registerAndGetToken(uniqueEmail(), "GARAGE");

        rest.exchange("/api/garages", HttpMethod.POST,
                new HttpEntity<>(garageBody(), bearerHeaders(token)), Map.class);

        var resp = rest.exchange("/api/garages", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> garages = resp.getBody();
        assertNotNull(garages);
        for (Map<String, Object> g : garages) {
            assertEquals(true, g.get("isApproved"));
        }
    }

    // Test 7: List garages — empty when none approved
    @Test
    void listGaragesEmptyWhenNoneApproved() {
        var resp = rest.exchange("/api/garages", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> garages = resp.getBody();
        assertNotNull(garages);
        for (Map<String, Object> g : garages) {
            assertEquals(true, g.get("isApproved"));
        }
    }

    // Test 8: Create then retrieve — full flow
    @Test
    void createGarageThenGet() {
        String token = registerAndGetToken(uniqueEmail(), "GARAGE");

        var createBody = Map.of(
                "businessName", "My Garage",
                "address", "Addr",
                "city", "City",
                "state", "State",
                "postalCode", "12345",
                "phone", "0741000000"
        );

        var createResp = rest.exchange("/api/garages", HttpMethod.POST,
                new HttpEntity<>(createBody, bearerHeaders(token)), Map.class);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());

        var getResp = rest.exchange("/api/garages/me", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)), Map.class);
        assertEquals(HttpStatus.OK, getResp.getStatusCode());
        assertEquals("My Garage", getResp.getBody().get("businessName"));
        assertEquals(false, getResp.getBody().get("isApproved"));
    }
}
