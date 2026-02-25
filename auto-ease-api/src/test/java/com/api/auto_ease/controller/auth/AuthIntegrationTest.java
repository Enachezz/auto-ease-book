package com.api.auto_ease.controller.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    @Test
    void registerThenLogin() {
        String email = uniqueEmail();
        var registerReq = Map.of(
                "email", email,
                "password", "password123",
                "fullName", "Test Owner",
                "userType", "CAR_OWNER"
        );

        var regResp = rest.postForEntity("/api/auth/register", registerReq, Map.class);
        assertEquals(HttpStatus.OK, regResp.getStatusCode());
        assertNotNull(regResp.getBody());
        assertNotNull(regResp.getBody().get("token"));
        assertEquals(email, regResp.getBody().get("email"));
        assertEquals("CAR_OWNER", regResp.getBody().get("userType"));
        assertNotNull(regResp.getBody().get("userId"));
        assertNotNull(regResp.getBody().get("fullName"));

        var loginReq = Map.of("email", email, "password", "password123");
        var loginResp = rest.postForEntity("/api/auth/login", loginReq, Map.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        assertNotNull(loginResp.getBody());
        assertNotNull(loginResp.getBody().get("token"));
    }

    @Test
    void registerDuplicateEmailReturns409() {
        String email = uniqueEmail();
        var req = Map.of("email", email, "password", "pass123", "fullName", "Dup", "userType", "GARAGE");

        rest.postForEntity("/api/auth/register", req, Map.class);
        var resp = rest.postForEntity("/api/auth/register", req, Map.class);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    @Test
    void registerMissingFieldsReturns400() {
        var req = Map.of("email", uniqueEmail());
        var resp = rest.postForEntity("/api/auth/register", req, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void loginWrongPasswordReturns401() {
        String email = uniqueEmail();
        var regReq = Map.of("email", email, "password", "pass123", "fullName", "User", "userType", "CAR_OWNER");
        rest.postForEntity("/api/auth/register", regReq, Map.class);

        var loginReq = Map.of("email", email, "password", "wrongpassword");
        var resp = rest.postForEntity("/api/auth/login", loginReq, Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void loginNonexistentEmailReturns401() {
        var req = Map.of("email", "nobody@nowhere.com", "password", "pass");
        var resp = rest.postForEntity("/api/auth/login", req, Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void authenticatedRequestWithValidToken() {
        String email = uniqueEmail();
        var regReq = Map.of("email", email, "password", "pass123", "fullName", "User", "userType", "CAR_OWNER");
        var regResp = rest.postForEntity("/api/auth/register", regReq, Map.class);
        String token = (String) regResp.getBody().get("token");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var entity = new HttpEntity<>(headers);

        var resp = rest.exchange("/api/profiles/me", HttpMethod.GET, entity, String.class);
        // Endpoint doesn't exist yet — security should let us through (not 401/403)
        assertTrue(
            resp.getStatusCode() != HttpStatus.UNAUTHORIZED && resp.getStatusCode() != HttpStatus.FORBIDDEN,
            "Expected security to pass (got " + resp.getStatusCode() + ")"
        );
    }

    @Test
    void unauthenticatedRequestReturns401() {
        var resp = rest.getForEntity("/api/profiles/me", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }
}
