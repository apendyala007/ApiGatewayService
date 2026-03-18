package com.bharatbank.gateway.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServerConfig {
    // Removed custom NettyReactiveWebServerFactory bean
    // Spring Boot will auto-configure the reactive web server
}
