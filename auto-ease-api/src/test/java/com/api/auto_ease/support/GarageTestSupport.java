package com.api.auto_ease.support;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class GarageTestSupport {

    private GarageTestSupport() {
    }

    public static String adminToken(TestRestTemplate rest) {
        var loginReq = Map.of("email", "admin@auto-ease.local", "password", "password");
        var loginResp = rest.postForEntity("/api/auth/login", loginReq, Map.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        assertNotNull(loginResp.getBody());
        return (String) loginResp.getBody().get("token");
    }

    public static void approveGarage(TestRestTemplate rest, String garageId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken(rest));
        var resp = rest.exchange("/api/garages/" + garageId + "/approve", HttpMethod.PATCH,
                new HttpEntity<>(null, headers), Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }
}
