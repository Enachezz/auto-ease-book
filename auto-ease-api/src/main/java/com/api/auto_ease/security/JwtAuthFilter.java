package com.api.auto_ease.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractBearerToken(request);
        if (token == null && isOpenApiDocumentationPath(request)) {
            token = readCookie(request, SwaggerAccessCookies.ACCESS_TOKEN);
        }

        if (token != null && jwtService.isValid(token)) {
            Claims claims = jwtService.validateToken(token);
            String uuid = claims.getSubject();
            String role = claims.get("role", String.class);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var auth = new UsernamePasswordAuthenticationToken(uuid, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Cookie-based JWT is accepted only for OpenAPI/Swagger routes so API clients keep using {@code Authorization} only
     * (avoids CSRF on state-changing {@code /api/**} requests).
     */
    private static boolean isOpenApiDocumentationPath(HttpServletRequest request) {
        String path = pathWithinApplication(request);
        return path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui/")
                || path.equals("/swagger-ui.html");
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
