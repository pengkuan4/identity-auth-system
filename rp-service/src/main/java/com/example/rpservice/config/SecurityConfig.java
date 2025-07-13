package com.example.rpservice.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/public", "/login/oauth2/code/**", "/verify", "/index.html", "/start-authorization").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(new AuthenticationSuccessHandler() {
                            @Override
                            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.Authentication authentication) throws IOException {
                                String registrationId = request.getParameter("registration_id"); // 获取 registrationId
                                if (registrationId == null) {
                                    registrationId = (String) request.getSession().getAttribute("oauth2_registration_id");
                                }
                                Boolean verified = (Boolean) request.getSession().getAttribute("verified");
                                String oauth2State = (String) request.getSession().getAttribute("oauth2_state");
                                String stateParam = request.getParameter("state");
                                System.out.println("Debug - registrationId: " + registrationId + ", verified: " + verified + ", oauth2State: " + oauth2State + ", stateParam: " + stateParam);
                                if ("op-service".equals(registrationId) && verified != null && verified && oauth2State != null && oauth2State.equals(stateParam)) {
                                    request.getSession().removeAttribute("verified");
                                    request.getSession().removeAttribute("oauth2_state");
                                    request.getSession().removeAttribute("oauth2_registration_id");
                                    response.sendRedirect("/resource");
                                } else {
                                    response.sendRedirect("/verify");
                                }
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            System.err.println("OAuth2 Failure: " + exception.getMessage() + " at " + request.getRequestURI());
                            response.sendRedirect("/public?error=true&message=" + java.net.URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8));
                        })
                )
                .sessionManagement(session -> session
                        .sessionFixation().none()
                );

        return http.build();
    }
}