package com.wizz.card_management.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.config.Customizer;
import org.springframework.http.MediaType;

@Configuration
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
                List.of(allowedOrigin)
        );

        config.setAllowedMethods(
                List.of("POST", "OPTIONS")
        );

        config.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "X-Request-Id",
                        "X-Idempotency-Key",
                        "X-Channel"
                )
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

        @Bean
        public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> {
                String requestId = request.getHeader("X-Request-Id");

                if (requestId != null) {
                response.setHeader("X-Request-Id", requestId);
                }

                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                Map<String, String> body = new LinkedHashMap<>();

                if (requestId != null) {
                body.put("referenceId", requestId);
                }

                body.put("responseCode", "98");
                body.put("responseDesc", "Unauthorized");

                new ObjectMapper().writeValue(response.getWriter(), body);
        };
        }

        @Bean
        public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
                String requestId = request.getHeader("X-Request-Id");

                if (requestId != null) {
                response.setHeader("X-Request-Id", requestId);
                }

                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                Map<String, String> body = new LinkedHashMap<>();

                if (requestId != null) {
                body.put("referenceId", requestId);
                }

                body.put("responseCode", "98");
                body.put("responseDesc", "Forbidden");

                new ObjectMapper().writeValue(response.getWriter(), body);
        };
        }
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                .headers(headers ->
                        headers.httpStrictTransportSecurity(hsts ->
                                hsts
                                        .includeSubDomains(true)
                                        .maxAgeInSeconds(31536000)
                        )
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/h2-console/**"
                        )
                        .denyAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/v1/cards"
                        )
                        .hasAuthority("SCOPE_cards:write")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/v1/cards/details"
                        )
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/v1/cards/setStatus"
                        )
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/v1/txnControls"
                        )
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/v1/txnControls/set"
                        )
                        .authenticated()

                        .anyRequest()
                        .authenticated()
                )

                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint(
                                        authenticationEntryPoint()
                                )
                                .accessDeniedHandler(
                                        accessDeniedHandler()
                                )
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                );

        return http.build();
    }

        @Value("${app.cors.allowed-origin}")
        private String allowedOrigin;

}