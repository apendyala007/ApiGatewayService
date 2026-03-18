package com.bharatbank.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String START_TIME_HEADER = "X-Start-Time";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = generateRequestId();
        
        long startTime = System.currentTimeMillis();
        
        logRequest(request, requestId);
        
        ServerHttpRequest modifiedRequest = request.mutate()
            .header(REQUEST_ID_HEADER, requestId)
            .header(START_TIME_HEADER, String.valueOf(startTime))
            .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build())
            .doFinally(signalType -> {
                ServerHttpResponse response = exchange.getResponse();
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                logResponse(response, requestId, duration);
            });
    }

    private void logRequest(ServerHttpRequest request, String requestId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String method = request.getMethod().name();
        String uri = request.getPath().value();
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String clientIp = getClientIp(request);
        
        log.info("Request [{}] - {} {} | IP: {} | User-Agent: {} | Time: {}", 
            requestId, method, uri, clientIp, userAgent, timestamp);
        
        if (log.isDebugEnabled()) {
            request.getHeaders().forEach((name, values) -> 
                log.debug("Request Header [{}] {}: {}", requestId, name, values));
        }
    }

    private void logResponse(ServerHttpResponse response, String requestId, long duration) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
        
        log.info("Response [{}] - Status: {} | Duration: {}ms | Time: {}", 
            requestId, statusCode, duration, timestamp);
        
        if (log.isDebugEnabled()) {
            response.getHeaders().forEach((name, values) -> 
                log.debug("Response Header [{}] {}: {}", requestId, name, values));
        }
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null 
            ? request.getRemoteAddress().getAddress().getHostAddress() 
            : "unknown";
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
