package com.cloudGateway.demo.filter;

import com.cloudGateway.demo.dto.TokenValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private RouteValidator validator;

    private final WebClient webClient;

    public AuthenticationFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClient = webClientBuilder.baseUrl("http://localhost:8085").build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        System.out.println("Gateway teraz filtruje");
        return (exchange, chain) -> {


            if (!validator.isSecured.test(exchange.getRequest())) {
                return chain.filter(exchange);
            }

            HttpCookie authCookie = exchange.getRequest().getCookies().getFirst("auth_token");
            if (authCookie == null) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authCookie.getValue();

            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/auth/validateToken")
                            .queryParam("token", token)
                            .build())
                    .retrieve()
                    .bodyToMono(TokenValidationResponse.class)
                    .flatMap(body -> {
                        if (body == null || !body.isValid()) {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }

                        String userId = String.valueOf(body.getUserId());

                        ServerHttpRequest mutatedRequest = exchange.getRequest()
                                .mutate()
                                .header("X-User-Id", userId)
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    })
                    .onErrorResume(err -> {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    public static class Config {}
}
