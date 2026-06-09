package com.api.auto_ease.security;

import com.api.auto_ease.domain.appUser.AppUserType;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * {@link PreAuthorize} SpEL snippets for {@link AppUserType}.
 * Each literal must match {@link AppUserType#name()} — enforced by {@code AppUserTypeSecurityExpressionsTest}.
 */
public final class AppUserTypeSecurityExpressions {

    private AppUserTypeSecurityExpressions() {
    }

    public static final String HAS_ROLE_CAR_OWNER = "hasRole('CAR_OWNER')";
    public static final String HAS_ROLE_GARAGE = "hasRole('GARAGE')";
    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";
    public static final String HAS_ROLE_GARAGE_OR_ADMIN = "hasRole('GARAGE') or hasRole('ADMIN')";
}
