package com.api.auto_ease.security;

import com.api.auto_ease.domain.appUser.AppUserType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppUserTypeSecurityExpressionsTest {

    @Test
    void preAuthorizeLiteralsMatchAppUserTypeNames() {
        assertEquals("hasRole('" + AppUserType.CAR_OWNER.name() + "')", AppUserTypeSecurityExpressions.HAS_ROLE_CAR_OWNER);
        assertEquals("hasRole('" + AppUserType.GARAGE.name() + "')", AppUserTypeSecurityExpressions.HAS_ROLE_GARAGE);
        assertEquals("hasRole('" + AppUserType.ADMIN.name() + "')", AppUserTypeSecurityExpressions.HAS_ROLE_ADMIN);
        assertEquals("hasRole('" + AppUserType.GARAGE.name() + "') or hasRole('" + AppUserType.ADMIN.name() + "')",
                AppUserTypeSecurityExpressions.HAS_ROLE_GARAGE_OR_ADMIN);
    }
}
