package com.bharatbank.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/auth/**", "/actuator/**", "/fallback/**").permitAll()
                .pathMatchers("/api/users").hasAnyRole("USER","ADMIN")
                .pathMatchers("/api/customers", "/api/customers/**").hasAnyRole("USER", "ADMIN", "BANK_MANAGER")
                .pathMatchers("/api/kyc", "/api/kyc/**").hasAnyRole("USER", "ADMIN", "BANK_MANAGER")
                .pathMatchers("/api/accounts/**", "/api/transactions/**").hasRole("ACCOUNT_SERVICE")
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .jwtDecoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            ))
            .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return token -> Mono.fromCallable(() -> decodeJwt(token));
    }

    private Jwt decodeJwt(String token) {
        try {
            SecretKey secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);

            Claims claims = jws.getBody();
            Jwt.Builder builder = Jwt.withTokenValue(token)
                .headers(headers -> headers.putAll(jws.getHeader()))
                .claims(jwtClaims -> jwtClaims.putAll(claims));

            Date issuedAt = claims.getIssuedAt();
            if (issuedAt != null) {
                builder.issuedAt(issuedAt.toInstant());
            }

            Date expiresAt = claims.getExpiration();
            if (expiresAt != null) {
                builder.expiresAt(expiresAt.toInstant());
            }

            Date notBefore = claims.getNotBefore();
            if (notBefore != null) {
                builder.notBefore(notBefore.toInstant());
            }

            return builder.build();
        } catch (Exception ex) {
            throw new BadJwtException("Failed to validate the token", ex);
        }
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        return jwt -> Mono.just(new JwtAuthenticationToken(jwt, extractAuthorities(jwt), jwt.getSubject()));
    }

    private List<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> authorities = new LinkedHashSet<>();

        addAuthorities(authorities, jwt.getClaim("roles"), false);
        addAuthorities(authorities, jwt.getClaim("authorities"), false);
        addAuthorities(authorities, jwt.getClaimAsStringList("scope"), true);

        addAuthority(authorities, jwt.getClaimAsString("role"));
        addAuthority(authorities, jwt.getClaimAsString("roleName"));

        return authorities.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }

    private void addAuthorities(Set<String> target, Object values, boolean prefixRole) {
        if (values == null) {
            return;
        }

        if (values instanceof String value) {
            addAuthorityValue(target, value, prefixRole);
            return;
        }

        if (!(values instanceof Collection<?> collection)) {
            return;
        }

        collection.forEach(value -> {
            if (value instanceof String stringValue) {
                addAuthorityValue(target, stringValue, prefixRole);
            } else if (value instanceof Map<?, ?> mapValue) {
                Object authority = mapValue.get("authority");
                if (authority instanceof String authorityValue) {
                    addAuthorityValue(target, authorityValue, prefixRole);
                }
            }
        });
    }

    private void addAuthority(Set<String> target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        addAuthorityValue(target, value, true);
    }

    private void addAuthorityValue(Set<String> target, String value, boolean prefixRole) {
        if (value == null || value.isBlank()) {
            return;
        }
        target.add(prefixRole && !value.startsWith("ROLE_") ? "ROLE_" + value : value);
    }
}
