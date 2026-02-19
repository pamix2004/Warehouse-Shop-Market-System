package com.cloudGateway.demo.filter;

import com.cloudGateway.demo.dto.TokenValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private RouteValidator validator;

    private final WebClient webClient;

    public AuthenticationFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        //JAK NIE DZIAŁA to przez to
        this.webClient = webClientBuilder.baseUrl("http://auth-service:8080").build();
    }


    private Mono<Void> redirectToLogin(ServerWebExchange exchange, String message) {
        String redirectUrl = "/auth/login?information=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

        exchange.getResponse().setStatusCode(HttpStatus.FOUND); // 302
        exchange.getResponse().getHeaders().set(HttpHeaders.LOCATION, redirectUrl);

        return exchange.getResponse().setComplete();
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
                return redirectToLogin(exchange, "You need to log in, no auth_token");
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
                            return redirectToLogin(exchange, "You need to log in,expired token");
                        }

                        String userId = String.valueOf(body.getUserId());

                        ServerHttpRequest mutatedRequest = exchange.getRequest()
                                .mutate()
                                .headers(h -> {
                                    h.remove("X-User-Id");      // remove any client-sent spoofed one
                                    h.set("X-User-Id", userId); // set your trusted one
                                })
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    })
                    .onErrorResume(err -> {
                        // This will print the REAL error in your Docker logs
                        err.printStackTrace();
                        return redirectToLogin(exchange, "Error: " + err.getMessage());
                    });
        };
    }

    public static class Config {}
}
