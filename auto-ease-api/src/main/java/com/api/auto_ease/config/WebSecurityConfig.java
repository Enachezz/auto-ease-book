package com.api.auto_ease.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.util.List;

@EnableSpringDataWebSupport
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds=604800)
public class WebSecurityConfig {


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();

        // CRITICAL: When allowCredentials=true, browsers reject wildcard origins (*)
        // Must use explicit origins or origin patterns
        // Spring's setAllowedOriginPatterns supports patterns with * for ports
        // Explicit localhost patterns for common dev ports + production domains
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",          // Angular dev server (any port: 4200, 4000, etc.)
                "http://127.0.0.1:*",          // Alternative localhost format
                "http://0.0.0.0:*",            // Alternative binding address
                "https://*.champdle.com",      // Production subdomains (api.champdle.com, etc.)
                "https://champdle.com",        // Production root domain
                "https://www.champdle.com"     // Production www domain
        ));

        configuration.setAllowedMethods(List.of("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Cache-Control",
                "Content-Type",
                "X-Requested-With",
                "Access-Control-Allow-Credentials",
                "Origin",
                "Accept"
        ));
        configuration.setExposedHeaders(List.of(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieMaxAge(604800); // 1 hour
        serializer.setUseHttpOnlyCookie(true); // HttpOnly flag
        return serializer;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, DataSource dataSource) throws Exception {
        auth.jdbcAuthentication().dataSource(dataSource).passwordEncoder(new BCryptPasswordEncoder());
    }
}
