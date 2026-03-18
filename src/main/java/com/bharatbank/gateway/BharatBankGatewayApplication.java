package com.bharatbank.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration",
    "org.springframework.cloud.autoconfigure.LifecycleMvcEndpointAutoConfiguration",
    "org.springframework.cloud.autoconfigure.RefreshAutoConfiguration",
    "org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration",
    "org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryClientAutoConfiguration"
})
public class BharatBankGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(BharatBankGatewayApplication.class, args);
    }
}
