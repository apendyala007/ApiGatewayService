package com.bharatbank.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Autowired(required = false)
    private ReactiveJwtDecoder jwtDecoder;

    public AuthenticationFilter() {
        super(Config.class);
    }

    private static final List<String> EXCLUDED_PATHS = List.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/forgot-password",
        "/actuator/health",
        "/actuator/info"
    );

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            if (isExcludedPath(path)) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return handleError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // Try OAuth2 JWT validation first (if available), fall back to custom secret
            if (jwtDecoder != null) {
                return validateWithOAuth2(exchange, chain, request, authHeader, token);
            } else {
                return validateWithCustomSecret(exchange, chain, request, authHeader, token);
            }
        };
    }

    private Mono<Void> validateWithOAuth2(ServerWebExchange exchange, 
                                          org.springframework.cloud.gateway.filter.GatewayFilterChain chain,
                                          ServerHttpRequest request, 
                                          String authHeader, 
                                          String token) {
        return jwtDecoder.decode(token)
            .flatMap(jwt -> {
                // Extract claims from OAuth2 JWT
                String userId = jwt.getSubject();
                String email = jwt.getClaimAsString("email");
                String role = jwt.getClaimAsString("role");
                String customerId = jwt.getClaimAsString("customerId");

                ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-ID", userId)
                    .header("X-User-Role", role != null ? role : "")
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-Customer-ID", customerId != null ? customerId : "")
                    .header("Authorization", authHeader)
                    .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            })
            .onErrorResume(e -> {
                log.warn("OAuth2 JWT validation failed, trying custom secret: {}", e.getMessage());
                return validateWithCustomSecret(exchange, chain, request, authHeader, token);
            });
    }

    private Mono<Void> validateWithCustomSecret(ServerWebExchange exchange, 
                                                 org.springframework.cloud.gateway.filter.GatewayFilterChain chain,
                                                 ServerHttpRequest request, 
                                                 String authHeader, 
                                                 String token) {
        try {
            SecretKey key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-ID", claims.getSubject())
                .header("X-User-Role", claims.get("role", String.class))
                .header("X-User-Email", claims.get("email", String.class))
                .header("X-Customer-ID", claims.get("customerId", String.class))
                .header("Authorization", authHeader)
                .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return handleError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
        }
    }

    public static class Config {
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> handleError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format("{\"error\": \"%s\", \"status\": %d}", message, status.value());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
