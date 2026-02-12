package com.cloudGateway.demo.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> openApiEndpoints = List.of(
            "/auth/register",
            "/auth/validateToken",
            "/auth/login",
            "/auth/verify",
            "/auth/logout",
            "/auth/cookieTest",
            "/jwt/createJWTToken",
            "/jwt/verifyJWTToken",
            "/email/sendEmail",
            "/eureka",
            "/",
            "/payment/stripeWebhook",
            "/payment/create-checkout-session",
            "/offer/testCSS",
            "/css/",
            "/js/"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> {
                String path = request.getURI().getPath();
                return openApiEndpoints
                        .stream()
                        .noneMatch(uri -> {
                            // If the open endpoint is just "/", only match exact "/"
                            if (uri.equals("/")) {
                                return path.equals("/");
                            }
                            // Otherwise check if the path starts with the open endpoint
                            return path.startsWith(uri);
                        });
            };
}