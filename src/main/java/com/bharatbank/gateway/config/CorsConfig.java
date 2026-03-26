package com.bharatbank.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Allow credentials (cookies, authorization headers)
        corsConfig.setAllowCredentials(true);
        
        // Allowed origins - update for production environment
        corsConfig.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",        // React dev server
            "http://localhost:4200",        // Angular dev server
            "https://bharatbank.com",         // Production domain
            "https://app.bharatbank.com",     // Production app
            "https://admin.bharatbank.com",   // Admin panel
            "https://*.bharatbank.com"        // Subdomains (if needed)
        ));
        
        // Allowed HTTP methods
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allowed headers - explicitly define for security
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Correlation-ID",
            "Idempotency-Key",
            "X-API-Key",
            "X-Client-Version"
        ));
        
        // Exposed headers - headers clients can access
        corsConfig.setExposedHeaders(Arrays.asList(
            "X-Total-Count",
            "X-Response-Time",
            "X-Correlation-ID",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset",
            "X-RateLimit-Limit"
        ));
        
        // Max age for preflight cache (1 hour)
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
