package com.bharatbank.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            
            // Prevent MIME type sniffing
            headers.add("X-Content-Type-Options", "nosniff");
            
            // Prevent clickjacking
            headers.add("X-Frame-Options", "DENY");
            
            // XSS Protection
            headers.add("X-XSS-Protection", "1; mode=block");
            
            // Referrer Policy
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // Content Security Policy
            headers.add("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';");
            
            // Permissions Policy
            headers.add("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            
            // Strict Transport Security (HTTPS only)
            headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }));
    }

    @Override
    public int getOrder() {
        // Run late in the filter chain (after response is generated)
        return Ordered.LOWEST_PRECEDENCE;
    }
}
