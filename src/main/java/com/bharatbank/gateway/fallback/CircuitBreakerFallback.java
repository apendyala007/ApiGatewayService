package com.bharatbank.gateway.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class CircuitBreakerFallback {

    @GetMapping("/user-service")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        log.warn("User Service is unavailable. Fallback activated.");
        
        Map<String, Object> response = createFallbackResponse(
            "User Service", 
            "Authentication and user management services are temporarily unavailable. Please try again later.",
            "https://support.bharatbank.com/user-service"
        );
        
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response));
    }

    @GetMapping("/customer-service")
    public Mono<ResponseEntity<Map<String, Object>>> customerServiceFallback() {
        log.warn("Customer Service is unavailable. Fallback activated.");
        
        Map<String, Object> response = createFallbackResponse(
            "Customer Service", 
            "Customer management and KYC services are temporarily unavailable. Please try again later.",
            "https://support.bharatbank.com/customer-service"
        );
        
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response));
    }

    @GetMapping("/account-service")
    public Mono<ResponseEntity<Map<String, Object>>> accountServiceFallback() {
        log.warn("Account Service is unavailable. Fallback activated.");
        
        Map<String, Object> response = createFallbackResponse(
            "Account Service", 
            "Account and transaction services are temporarily unavailable. Please try again later.",
            "https://support.bharatbank.com/account-service"
        );
        
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response));
    }

    @GetMapping("/{serviceName}")
    public Mono<ResponseEntity<Map<String, Object>>> genericFallback(@PathVariable String serviceName) {
        log.warn("Service '{}' is unavailable. Generic fallback activated.", serviceName);
        
        Map<String, Object> response = createFallbackResponse(
            serviceName, 
            String.format("The %s service is temporarily unavailable. Please try again later.", serviceName),
            "https://support.bharatbank.com"
        );
        
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response));
    }

    private Map<String, Object> createFallbackResponse(String serviceName, String message, String supportUrl) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", message);
        response.put("service", serviceName);
        response.put("supportUrl", supportUrl);
        response.put("retryAfter", "60");
        response.put("circuitBreakerStatus", "OPEN");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("gateway", "bharatbank-api-gateway");
        metadata.put("version", "1.0.0");
        response.put("metadata", metadata);
        
        return response;
    }
}
