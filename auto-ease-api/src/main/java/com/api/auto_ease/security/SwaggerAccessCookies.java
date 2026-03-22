package com.api.auto_ease.security;

public final class SwaggerAccessCookies {

    /** HttpOnly cookie carrying the JWT for OpenAPI/Swagger routes only (see {@link JwtAuthFilter}). */
    public static final String ACCESS_TOKEN = "access_token";

    private SwaggerAccessCookies() {
    }
}
