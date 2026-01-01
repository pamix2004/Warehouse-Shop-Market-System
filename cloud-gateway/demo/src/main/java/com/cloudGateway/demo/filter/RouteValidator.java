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
            "/auth/cookieTest",
            "/jwt/createJWTToken",
            "/jwt/verifyJWTToken",
            "/email/sendEmail",
            "/eureka"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));

}
