package com.politechnika.warehouseManagement;

import jakarta.servlet.ServletException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Implement the AuthenticationFailureHandler interface to create a custom authentication failure handler
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    // Override the onAuthenticationFailure method to handle authentication failure
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String errorMessage;

        if (exception instanceof DisabledException) {
            System.out.println("Account is not active");
            errorMessage = "Your account is not active. Please contact support.";
        } else if (exception instanceof BadCredentialsException) {
            errorMessage = "Invalid username or password.";
        } else {
            errorMessage = "Authentication failed: " + exception.getMessage();
        }

        // Encode the final message and redirect
        errorMessage = java.net.URLEncoder.encode(errorMessage, "UTF-8");


        response.sendRedirect("/login?error=" + errorMessage);

    }


}