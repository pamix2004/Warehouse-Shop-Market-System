package com.cloudGateway.demo.filter;

import com.cloudGateway.demo.dto.TokenValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private RouteValidator validator;

    public AuthenticationFilter(){
        super(Config.class);
    }
    @Autowired
    private RestTemplate template;

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            if (!validator.isSecured.test(exchange.getRequest())) {
                return chain.filter(exchange);
            }

            // get auth_token cookie
            HttpCookie authCookie = exchange.getRequest()
                    .getCookies()
                    .getFirst("auth_token");

            if (authCookie == null) {
                System.out.println("Authorization token is missing");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authCookie.getValue();

            try {
                ResponseEntity<TokenValidationResponse> response =
                        template.getForEntity(
                                "http://localhost:8085/auth/validateToken?token={token}",
                                TokenValidationResponse.class,
                                token
                        );

                TokenValidationResponse body = response.getBody();

                if (body == null || !body.isValid()) {
                    throw new RuntimeException("Invalid token");
                }

                int userId = body.getUserId();
                System.out.println("User authenticated, userId=" + userId);

                // add header to downstream request
                ServerHttpRequest mutatedRequest = exchange.getRequest()
                        .mutate()
                        .header("X-User-Id", String.valueOf(userId))
                        .build();

                ServerWebExchange mutatedExchange = exchange
                        .mutate()
                        .request(mutatedRequest)
                        .build();

                return chain.filter(mutatedExchange);

            } catch (Exception e) {
                System.out.println("invalid access...!");
                System.out.println("un authorized access to application");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    public static class Config{

    }
}
