package com.api.auto_ease.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SwaggerAdminDocsIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String uniqueEmail() {
        return "swagger-test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
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

    @Test
    void swaggerUiUnauthenticatedReturns401() {
        var resp = rest.getForEntity("/swagger-ui.html", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void swaggerUiWithCarOwnerTokenReturns403() {
        String token = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var resp = rest.exchange("/swagger-ui.html", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void swaggerUiWithGarageTokenReturns403() {
        String token = registerAndGetToken(uniqueEmail(), "GARAGE");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var resp = rest.exchange("/swagger-ui.html", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void openApiJsonWithGarageTokenReturns403() {
        String token = registerAndGetToken(uniqueEmail(), "GARAGE");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var resp = rest.exchange("/v3/api-docs", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void swaggerUiWithSeededAdminCookieReturns200() {
        var loginReq = Map.of("email", "admin@auto-ease.local", "password", "password");
        var loginResp = rest.postForEntity("/api/auth/login", loginReq, Map.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());

        List<String> setCookies = loginResp.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookies);
        String accessCookie = setCookies.stream()
                .filter(c -> c.startsWith("access_token="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected Set-Cookie access_token from admin login"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, accessCookie.split(";", 2)[0]);
        var resp = rest.exchange("/swagger-ui.html", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().contains("swagger") || resp.getBody().contains("Swagger"),
                "Expected Swagger UI HTML");
    }
}
