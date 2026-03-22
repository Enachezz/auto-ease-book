package com.api.auto_ease.controller.auth;

import com.api.auto_ease.domain.appUser.AppUserType;
import com.api.auto_ease.dto.auth.AuthResponse;
import com.api.auto_ease.dto.auth.LoginRequest;
import com.api.auto_ease.dto.auth.RegisterRequest;
import com.api.auto_ease.security.JwtService;
import com.api.auto_ease.security.SwaggerAccessCookies;
import com.api.auto_ease.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @Operation(security = {})
    @PostMapping("/api/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(security = {})
    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        AuthResponse body = authService.login(request);
        boolean admin = body.getUserType() == AppUserType.ADMIN;
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (admin) {
            ResponseCookie cookie = ResponseCookie.from(SwaggerAccessCookies.ACCESS_TOKEN, body.getToken())
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ofMillis(jwtService.getExpirationMs()))
                    .sameSite("Lax")
                    .secure(httpRequest.isSecure())
                    .build();
            builder.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        AuthResponse clientBody = admin
                ? AuthResponse.builder().token(body.getToken()).build()
                : body;
        return builder.body(clientBody);
    }
}
