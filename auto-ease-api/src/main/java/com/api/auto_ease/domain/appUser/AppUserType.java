package com.api.auto_ease.domain.appUser;

/**
 * Stored on {@code APP_USER.type} and in the JWT {@code role} claim.
 * Spring Security {@code hasRole(...)} uses {@link #name()} without the {@code ROLE_} prefix
 * (authorities are {@code ROLE_ + name()}).
 */
public enum AppUserType {
    CAR_OWNER,
    GARAGE,
    ADMIN
}
