package com.api.auto_ease.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ProfileAndReferenceDataIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private Map<String, Object> registerAndGetBody(String email, String fullName, String userType) {
        var req = Map.of(
                "email", email,
                "password", "pass123",
                "fullName", fullName,
                "userType", userType
        );
        var resp = rest.postForEntity("/api/auth/register", req, Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return resp.getBody();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    // --- Test 1: Get own profile (authenticated) ---
    @Test
    void getOwnProfile() {
        String email = uniqueEmail();
        var auth = registerAndGetBody(email, "Profile Owner", "CAR_OWNER");
        String token = (String) auth.get("token");

        var resp = rest.exchange("/api/profiles/me", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)), Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body);
        assertNotNull(body.get("id"));
        assertNotNull(body.get("userId"));
        assertEquals("Profile Owner", body.get("fullName"));
        assertEquals(email, body.get("email"));
        assertEquals("CAR_OWNER", body.get("userType"));
    }

    // --- Test 2: Update own profile ---
    @Test
    void updateOwnProfile() {
        String email = uniqueEmail();
        var auth = registerAndGetBody(email, "Original Name", "GARAGE");
        String token = (String) auth.get("token");
        HttpHeaders headers = bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var updateReq = Map.of("fullName", "Updated Name", "phone", "+40712345678");
        var putResp = rest.exchange("/api/profiles/me", HttpMethod.PUT,
                new HttpEntity<>(updateReq, headers), Map.class);
        assertEquals(HttpStatus.OK, putResp.getStatusCode());

        var getResp = rest.exchange("/api/profiles/me", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)), Map.class);
        assertEquals(HttpStatus.OK, getResp.getStatusCode());
        Map<String, Object> body = getResp.getBody();
        assertEquals("Updated Name", body.get("fullName"));
        assertEquals("+40712345678", body.get("phone"));
        assertEquals(email, body.get("email"));
        assertEquals("GARAGE", body.get("userType"));
    }

    // --- Test 3: Profile — unauthenticated ---
    @Test
    void profileUnauthenticatedReturns401() {
        var resp = rest.getForEntity("/api/profiles/me", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    // --- Test 4: List car makes ---
    @Test
    void listCarMakes() {
        var resp = rest.getForEntity("/api/car-makes", List.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<?> makes = resp.getBody();
        assertNotNull(makes);
        assertEquals(15, makes.size());
    }

    // --- Test 5: List models for a make (Toyota) ---
    @Test
    void listModelsForToyota() {
        var makesResp = rest.getForEntity("/api/car-makes", List.class);
        List<Map<String, Object>> makes = makesResp.getBody();

        String toyotaId = makes.stream()
                .filter(make -> "Toyota".equals(make.get("name")))
                .map(make -> make.get("id").toString())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Toyota not found in car makes"));

        var modelsResp = rest.getForEntity("/api/car-makes/" + toyotaId + "/models", List.class);
        assertEquals(HttpStatus.OK, modelsResp.getStatusCode());
        List<Map<String, Object>> models = modelsResp.getBody();
        assertNotNull(models);
        assertEquals(5, models.size());

        List<String> modelNames = models.stream().map(model -> (String) model.get("name")).toList();
        assertTrue(modelNames.contains("Camry"));
        assertTrue(modelNames.contains("Corolla"));
        assertTrue(modelNames.contains("RAV4"));
    }

    // --- Test 6: Models for nonexistent make ---
    @Test
    void modelsForNonexistentMakeReturnsEmptyList() {
        var resp = rest.getForEntity(
                "/api/car-makes/00000000-0000-0000-0000-000000000000/models", List.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().isEmpty());
    }

    // --- Test 7: List service categories ---
    @Test
    void listServiceCategories() {
        var resp = rest.getForEntity("/api/service-categories", List.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> categories = resp.getBody();
        assertNotNull(categories);
        assertEquals(10, categories.size());

        Map<String, Object> first = categories.get(0);
        assertNotNull(first.get("id"));
        assertNotNull(first.get("name"));
        assertNotNull(first.get("description"));
        assertNotNull(first.get("icon"));
    }

    // --- Test 8: Register then get profile (full flow) ---
    @Test
    void registerAndGetProfile() {
        String email = uniqueEmail();
        var auth = registerAndGetBody(email, "Test User", "CAR_OWNER");
        String token = (String) auth.get("token");

        var resp = rest.exchange("/api/profiles/me", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)), Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<String, Object> body = resp.getBody();
        assertEquals("Test User", body.get("fullName"));
        assertEquals("CAR_OWNER", body.get("userType"));
    }
}
