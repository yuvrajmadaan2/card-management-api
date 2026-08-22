package com.wizz.card_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
                List.of("https://portal.partner-forex.com")
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
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("groups");
        authoritiesConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return converter;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {

        return (request, response, authenticationException) -> {

            String requestId =
                    request.getHeader("X-Request-Id");

            if (requestId != null && !requestId.isBlank()) {
                response.setHeader(
                        "X-Request-Id",
                        requestId
                );
            }

            response.setStatus(401);
            response.setContentType("application/json");

            String json =
                    "{\"referenceId\":\"" +
                    requestId +
                    "\",\"responseCode\":\"98\",\"responseDesc\":\"Unauthorized\"}";

            response.getWriter().write(json);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {

        return (request, response, accessDeniedException) -> {

            String requestId =
                    request.getHeader("X-Request-Id");

            if (requestId != null && !requestId.isBlank()) {
                response.setHeader(
                        "X-Request-Id",
                        requestId
                );
            }

            response.setStatus(403);
            response.setContentType("application/json");

            String json =
                    "{\"referenceId\":\"" +
                    requestId +
                    "\",\"responseCode\":\"98\",\"responseDesc\":\"Forbidden\"}";

            response.getWriter().write(json);
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
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/v1/cards"
                        )
                        .hasAuthority("SCOPE_cards:write")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/cards/details"
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
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        return http.build();
    }
}