package com.bharatbank.gateway.config;

import com.bharatbank.gateway.filter.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // User Service Routes - NO auth filter for login/register
            .route("user-service", r -> r
                .path("/api/auth/**", "/api/users/**")
                .filters(f -> f.stripPrefix(0)
                    .prefixPath("/bharatbank-user-service"))
                .uri("http://localhost:8081"))
            
            // Customer Service Routes
            .route("customer-service", r -> r
                .path("/api/customers/**", "/api/kyc/**")
                .filters(f -> f.stripPrefix(0)
                    .prefixPath("/bharatbank-customer-service")
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                .uri("http://localhost:8083"))
            
            // Account Service Routes
            .route("account-service", r -> r
                .path("/api/accounts/**", "/api/transactions/**")
                .filters(f -> f
                    .prefixPath("/bharatbank-account-service")
                    .circuitBreaker(config -> config
                        .setName("account-service")
                        .setFallbackUri("forward:/fallback/account-service"))
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                .uri("http://localhost:8082"))
            
            // Payment Service Routes
            .route("payment-service", r -> r
                .path("/api/payments/**")
                .filters(f -> f.stripPrefix(0)
                    .prefixPath("/bharatbank-payment-service")
                    .circuitBreaker(config -> config
                        .setName("payment-service")
                        .setFallbackUri("forward:/fallback/payment-service"))
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                .uri("http://localhost:8084"))
            
            // Beneficiary Service Routes
            .route("beneficiary-service", r -> r
                .path("/api/beneficiaries/**")
                .filters(f -> f.stripPrefix(0)
                    .prefixPath("/bharatbank-beneficiary-service")
                    .circuitBreaker(config -> config
                        .setName("beneficiary-service")
                        .setFallbackUri("forward:/fallback/beneficiary-service"))
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                .uri("http://localhost:8086"))
            
            // Card Service Routes
            .route("card-service", r -> r
                .path("/api/cards/**")
                .filters(f -> f.stripPrefix(0)
                    .prefixPath("/bharatbank-card-service")
                    .circuitBreaker(config -> config
                        .setName("card-service")
                        .setFallbackUri("forward:/fallback/card-service"))
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                .uri("lb://bharatbank-card-service"))
            
            .build();
    }
}
