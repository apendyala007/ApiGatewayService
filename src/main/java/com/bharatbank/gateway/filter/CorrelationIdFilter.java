package com.bharatbank.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Slf4j
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = getCorrelationId(exchange);
        
        // Add correlation ID to request headers for downstream services
        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();
        
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();
        
        // Store in attributes for logging
        mutatedExchange.getAttributes().put(CORRELATION_ID_KEY, correlationId);
        
        // Add correlation ID to response headers
        return chain.filter(mutatedExchange)
                .then(Mono.fromRunnable(() -> {
                    mutatedExchange.getResponse().getHeaders()
                            .add(CORRELATION_ID_HEADER, correlationId);
                }));
    }

    private String getCorrelationId(ServerWebExchange exchange) {
        // Check if correlation ID is already present in request
        String correlationId = exchange.getRequest().getHeaders()
                .getFirst(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isEmpty()) {
            // Generate new correlation ID
            correlationId = generateCorrelationId();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID from request: {}", correlationId);
        }
        
        return correlationId;
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public int getOrder() {
        // Run early in the filter chain (before authentication)
        return -100;
    }
}
