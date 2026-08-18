package com.wizz.card_management.controller;

import com.wizz.card_management.config.SecurityConfig;
import com.wizz.card_management.dto.request.CreateCardRequest;
import com.wizz.card_management.dto.response.CreateCardResponse;
import com.wizz.card_management.ratelimit.RateLimitService;
import com.wizz.card_management.service.CardCreateService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;



@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
        OAuth2ResourceServerAutoConfiguration.class,
        OAuth2ResourceServerWebSecurityAutoConfiguration.class
})

class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardCreateService cardCreateService;

    @MockitoBean
    private RateLimitService rateLimitService;



    private JwtAuthenticationToken authenticationWithScope() {

        Jwt jwt = new Jwt(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "partner-001",
                        "scope", "cards:write"
                )
        );

        return new JwtAuthenticationToken(
                jwt,
                List.of(
                        new SimpleGrantedAuthority(
                                "SCOPE_cards:write"
                        )
                )
        );
    }

    private String validJson() {

        return """
                {
                  "card": {
                    "cardProgramType": "D",
                    "cardType": "V",
                    "cardProgramId": "PROGRAM-001"
                  }
                }
                """;
    }

    @Test
    void validRequest_shouldReturn200() throws Exception {

        CreateCardResponse response =
                new CreateCardResponse();

        response.setCardId("CARD-001");
        response.setCardNumber("4111XXXXXXXX1111");
        response.setExpiryDate("07/2031");
        response.setReferenceId("REQ-001");
        response.setResponseCode("00");
        response.setResponseDesc(
                "Card created successfully"
        );

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardCreateService.createCard(
                any(CreateCardRequest.class),
                eq("REQ-001"),
                eq("KEY-001"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);

        mockMvc.perform(
                        post("/v1/cards")
                                .with(request -> {
                                request.setUserPrincipal(
                                        authenticationWithScope()
                                );
                                return request;
                                })
                                .header(
                                        "X-Request-Id",
                                        "REQ-001"
                                )
                                .header(
                                        "X-Idempotency-Key",
                                        "KEY-001"
                                )
                                .header(
                                        "X-Channel",
                                        "WEB"
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(validJson())
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                "X-Request-Id",
                                "REQ-001"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.responseCode"
                        ).value("00")
                );

        verify(cardCreateService).createCard(
                any(CreateCardRequest.class),
                eq("REQ-001"),
                eq("KEY-001"),
                eq("WEB"),
                eq("partner-001")
        );
    }

    @Test
    void missingRequiredField_shouldReturn400()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        String invalidJson = """
                {
                  "card": {
                    "cardProgramType": "D",
                    "cardType": "V"
                  }
                }
                """;

        mockMvc.perform(
                        post("/v1/cards")
                                .with(request -> {
                                request.setUserPrincipal(
                                        authenticationWithScope()
                                );
                                return request;
                                })
                                .header(
                                        "X-Request-Id",
                                        "REQ-002"
                                )
                                .header(
                                        "X-Idempotency-Key",
                                        "KEY-002"
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(invalidJson)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath(
                                "$.responseCode"
                        ).value("01")
                );

        verify(cardCreateService, never())
                .createCard(
                        any(),
                        anyString(),
                        anyString(),
                        any(),
                        anyString()
                );
    }


    @Test
    void duplicateIdempotency_shouldReturn409()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardCreateService.createCard(
                any(CreateCardRequest.class),
                eq("REQ-004"),
                eq("KEY-004"),
                any(),
                eq("partner-001")
        )).thenThrow(
                new com.wizz.card_management.exception
                        .IdempotencyConflictException()
        );

        mockMvc.perform(
                        post("/v1/cards")
                                .with(request -> {
                                request.setUserPrincipal(
                                        authenticationWithScope()
                                );
                                return request;
                                })
                                .header(
                                        "X-Request-Id",
                                        "REQ-004"
                                )
                                .header(
                                        "X-Idempotency-Key",
                                        "KEY-004"
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(validJson())
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.responseCode")
                                .value("409")
                );
    }

    @Test
    void rateLimitExceeded_shouldReturn429()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(false);

        mockMvc.perform(
                        post("/v1/cards")
                                .with(request -> {
                                request.setUserPrincipal(
                                        authenticationWithScope()
                                );
                                return request;
                                })
                                .header(
                                        "X-Request-Id",
                                        "REQ-005"
                                )
                                .header(
                                        "X-Idempotency-Key",
                                        "KEY-005"
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(validJson())
                )
                .andExpect(
                        status().isTooManyRequests()
                )
                .andExpect(
                        header().string(
                                "X-Request-Id",
                                "REQ-005"
                        )
                );

        verifyNoInteractions(cardCreateService);
    }
}