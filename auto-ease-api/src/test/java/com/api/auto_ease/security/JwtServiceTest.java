package com.api.auto_ease.security;

import com.api.auto_ease.domain.appUser.AppUserType;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "auto-ease-jwt-test-secret-that-is-long-enough-for-hs256-algorithm",
                86400000L
        );
    }

    @Test
    void generateAndValidateToken() {
        String uuid = UUID.randomUUID().toString();
        String token = jwtService.generateToken(uuid, "test@test.com", AppUserType.CAR_OWNER);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);

        Claims claims = jwtService.validateToken(token);
        assertEquals(uuid, claims.getSubject());
        assertEquals("CAR_OWNER", claims.get("role"));
        assertEquals("test@test.com", claims.get("email"));
    }

    @Test
    void extractUuid() {
        String uuid = UUID.randomUUID().toString();
        String token = jwtService.generateToken(uuid, "test@test.com", AppUserType.GARAGE);

        assertEquals(uuid, jwtService.extractUuid(token));
    }

    @Test
    void isValidReturnsTrueForValidToken() {
        String token = jwtService.generateToken("uuid", "e@e.com", AppUserType.CAR_OWNER);
        assertTrue(jwtService.isValid(token));
    }

    @Test
    void isValidReturnsFalseForGarbage() {
        assertFalse(jwtService.isValid("not.a.jwt"));
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtService shortLived = new JwtService(
                "auto-ease-jwt-test-secret-that-is-long-enough-for-hs256-algorithm",
                -1000L
        );
        String token = shortLived.generateToken("uuid", "e@e.com", AppUserType.GARAGE);
        assertFalse(jwtService.isValid(token));
    }
}
