package com.api.auto_ease.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSeedPasswordTest {

    @Test
    void flywayAdminSeedPasswordIsDocumentedDefault() {
        String hash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
        assertTrue(new BCryptPasswordEncoder().matches("password", hash),
                "Update V8__seed_admin_user.sql if this fails — hash must match the seeded admin password");
    }
}
