package com.politechnika.warehouseManagement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {


/*
* @Override
protected void configure(HttpSecurity http) throws Exception {
    http.formLogin()
        .loginPage("/login")
        .failureHandler((request, response, exception) -> {
            if (exception instanceof InactiveUserException) {
                response.sendRedirect("/login?error=inactive");
            } else {
                response.sendRedirect("/login?error=true");
            }
        })
        .permitAll();
}

*
* */

    @Autowired
    CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/","/login" ,"/home", "/static/test","/register","/verify","css/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin((form) -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .failureHandler(customAuthenticationFailureHandler)
                        .permitAll()
                )

                .logout((logout) -> logout.permitAll())

        ;


        return http.build();
    }

    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }







}