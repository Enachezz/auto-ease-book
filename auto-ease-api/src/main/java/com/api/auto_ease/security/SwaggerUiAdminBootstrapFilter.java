package com.api.auto_ease.security;

import com.api.auto_ease.domain.appUser.AppUserType;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

/**
 * One-time browser bootstrap: {@code GET /swagger-ui.html?access_token=...} validates an admin JWT,
 * sets an HttpOnly cookie, then redirects to the UI without the token in the URL (avoids log/referrer leaks on assets).
 */
@RequiredArgsConstructor
public class SwaggerUiAdminBootstrapFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private static final List<String> SWAGGER_ENTRY_PATHS = List.of(
            "/swagger-ui.html",
            "/swagger-ui/index.html"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = pathWithinApplication(request);
        if (SWAGGER_ENTRY_PATHS.stream().noneMatch(path::equals)) {
            filterChain.doFilter(request, response);
            return;
        }

        String qp = request.getParameter(SwaggerAccessCookies.ACCESS_TOKEN);
        if (qp == null || qp.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.isValid(qp)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        Claims claims = jwtService.validateToken(qp);
        String role = claims.get("role", String.class);
        if (!AppUserType.ADMIN.name().equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin role required");
            return;
        }

        long maxAgeSec = Math.max(1L, (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000L);

        Cookie cookie = new Cookie(SwaggerAccessCookies.ACCESS_TOKEN, qp);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) maxAgeSec);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);

        String target = UriComponentsBuilder.fromPath(request.getContextPath() + "/swagger-ui/index.html")
                .build()
                .toUriString();
        response.sendRedirect(target);
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
