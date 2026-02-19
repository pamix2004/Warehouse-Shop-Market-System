package com.cloudGateway.demo.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate template(){
        return new RestTemplate();
    }

    @Bean
    @LoadBalanced // Spring will now see this and enable Service Discovery
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
