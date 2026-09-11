package com.wizz.card_management.controller;

import com.wizz.card_management.dto.response.SetCardStatusResponse;
import com.wizz.card_management.ratelimit.RateLimitService;
import com.wizz.card_management.service.CardStatusUpdateService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardStatusController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
        OAuth2ResourceServerAutoConfiguration.class,
        OAuth2ResourceServerWebSecurityAutoConfiguration.class
})
class CardStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardStatusUpdateService cardStatusUpdateService;

    @MockitoBean
    private RateLimitService rateLimitService;

    private JwtAuthenticationToken authentication() {

        Jwt jwt = new Jwt(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "partner-001"
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
                    "cardId": "CARD001",
                    "statusCode": "S",
                    "reasonCode": "CUSTREQ",
                    "remarks": "Temporary travel freeze"
                  }
                }
                """;
    }

    @Test
    void validRequest_shouldReturn200() throws Exception {

        SetCardStatusResponse response =
                new SetCardStatusResponse();

        response.setReferenceId("REQ-001");
        response.setCardNumber("4111XXXXXXXX1234");
        response.setCardProgramName(
                "WizzPlus Multicurrency Prepaid"
        );
        response.setCustomerId("0012342");
        response.setResponseCode("00");
        response.setResponseDesc(
                "Card status updated to TEMP SUSPENDED"
        );

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardStatusUpdateService.updateCardStatus(
                any(),
                eq("REQ-001"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);

        mockMvc.perform(
                        post("/v1/cards/setStatus")
                                .with(request -> {
                                    request.setUserPrincipal(
                                            authentication()
                                    );
                                    return request;
                                })
                                .header(
                                        "X-Request-Id",
                                        "REQ-001"
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
                )
                .andExpect(
                        jsonPath(
                                "$.referenceId"
                        ).value("REQ-001")
                )
                .andExpect(
                        jsonPath(
                                "$.cardNumber"
                        ).value("4111XXXXXXXX1234")
                );

        verify(cardStatusUpdateService).updateCardStatus(
                any(),
                eq("REQ-001"),
                eq("WEB"),
                eq("partner-001")
        );
    }

    @Test
    void missingReasonCode_shouldReturn200WithBusinessDecline()
            throws Exception {

        SetCardStatusResponse response =
                new SetCardStatusResponse();

        response.setReferenceId("REQ-002");
        response.setResponseCode("02");
        response.setResponseDesc(
                "Reason code is mandatory for the requested card status"
        );

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardStatusUpdateService.updateCardStatus(
                any(),
                eq("REQ-002"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);

        String jsonWithoutReasonCode = """
                {
                  "card": {
                    "cardId": "CARD001",
                    "statusCode": "B",
                    "remarks": "Block card"
                  }
                }
                """;

        mockMvc.perform(
                        post("/v1/cards/setStatus")
                                .with(request -> {
                                    request.setUserPrincipal(
                                            authentication()
                                    );
                                    return request;
                                })
                                .header(
                                        "X-Request-Id",
                                        "REQ-002"
                                )
                                .header(
                                        "X-Channel",
                                        "WEB"
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        jsonWithoutReasonCode
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.responseCode"
                        ).value("02")
                )
                .andExpect(
                        jsonPath(
                                "$.responseDesc"
                        ).value(
                                "Reason code is mandatory for the requested card status"
                        )
                );
    }

    @Test
    void invalidStatusCode_shouldReturn200WithBusinessDecline()
            throws Exception {

        SetCardStatusResponse response =
                new SetCardStatusResponse();

        response.setReferenceId("REQ-003");
        response.setResponseCode("01");
        response.setResponseDesc(
                "Invalid card status code"
        );

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardStatusUpdateService.updateCardStatus(
                any(),
                eq("REQ-003"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);

        String invalidJson = """
                {
                  "card": {
                    "cardId": "CARD001",
                    "statusCode": "X",
                    "reasonCode": "TEST"
                  }
                }
                """;

        mockMvc.perform(
                        post("/v1/cards/setStatus")
                                .with(request -> {
                                    request.setUserPrincipal(
                                            authentication()
                                    );
                                    return request;
                                })
                                .header(
                                        "X-Request-Id",
                                        "REQ-003"
                                )
                                .header(
                                        "X-Channel",
                                        "WEB"
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(invalidJson)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.responseCode"
                        ).value("01")
                );
    }

    @Test
    void cardNotFound_shouldReturn200WithBusinessDecline()
            throws Exception {

        SetCardStatusResponse response =
                new SetCardStatusResponse();

        response.setReferenceId("REQ-004");
        response.setResponseCode("10");
        response.setResponseDesc(
                "Card not found"
        );

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardStatusUpdateService.updateCardStatus(
                any(),
                eq("REQ-004"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);

        mockMvc.perform(
                        post("/v1/cards/setStatus")
                                .with(request -> {
                                    request.setUserPrincipal(
                                            authentication()
                                    );
                                    return request;
                                })
                                .header(
                                        "X-Request-Id",
                                        "REQ-004"
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
                        jsonPath(
                                "$.responseCode"
                        ).value("10")
                )
                .andExpect(
                        jsonPath(
                                "$.responseDesc"
                        ).value("Card not found")
                );
    }

    @Test
    void invalidRequest_shouldReturn400()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        String invalidJson = """
                {
                  "card": {
                    "cardId": "",
                    "statusCode": ""
                  }
                }
                """;

        mockMvc.perform(
                        post("/v1/cards/setStatus")
                                .with(request -> {
                                    request.setUserPrincipal(
                                            authentication()
                                    );
                                    return request;
                                })
                                .header(
                                        "X-Request-Id",
                                        "REQ-005"
                                )
                                .header(
                                        "X-Channel",
                                        "WEB"
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(invalidJson)
                )
                .andExpect(
                        status().isBadRequest()
                );

        verifyNoInteractions(
                cardStatusUpdateService
        );
    }

    @Test
    void rateLimitExceeded_shouldReturn429()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(false);

        mockMvc.perform(
                        post("/v1/cards/setStatus")
                                .with(request -> {
                                    request.setUserPrincipal(
                                            authentication()
                                    );
                                    return request;
                                })
                                .header(
                                        "X-Request-Id",
                                        "REQ-006"
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
                .andExpect(
                        status().isTooManyRequests()
                )
                .andExpect(
                        header().string(
                                "X-Request-Id",
                                "REQ-006"
                        )
                );

        verifyNoInteractions(
                cardStatusUpdateService
        );
    }

    @Test
    void missingRequestId_shouldReturn400()
            throws Exception {

        mockMvc.perform(
                        post("/v1/cards/setStatus")
                                .with(request -> {
                                    request.setUserPrincipal(
                                            authentication()
                                    );
                                    return request;
                                })
                                .header(
                                        "X-Channel",
                                        "WEB"
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(validJson())
                )
                .andExpect(
                        status().isBadRequest()
                );

        verifyNoInteractions(
                cardStatusUpdateService
        );
    }

    @Test
    void invalidTransition_shouldReturn200WithBusinessDecline()
            throws Exception {

        SetCardStatusResponse response =
                new SetCardStatusResponse();

        response.setReferenceId("REQ-007");
        response.setResponseCode("31");
        response.setResponseDesc(
                "Invalid card status transition"
        );

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardStatusUpdateService.updateCardStatus(
                any(),
                eq("REQ-007"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);

        mockMvc.perform(
                        post("/v1/cards/setStatus")
                                .with(request -> {
                                    request.setUserPrincipal(
                                            authentication()
                                    );
                                    return request;
                                })
                                .header(
                                        "X-Request-Id",
                                        "REQ-007"
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
                        jsonPath(
                                "$.responseCode"
                        ).value("31")
                );
    }

    @Test
    void internalException_shouldReturn500()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardStatusUpdateService.updateCardStatus(
                any(),
                eq("REQ-008"),
                eq("WEB"),
                eq("partner-001")
        )).thenThrow(
                new RuntimeException(
                        "Database unavailable"
                )
        );

        mockMvc.perform(
                        post("/v1/cards/setStatus")
                                .with(request -> {
                                    request.setUserPrincipal(
                                            authentication()
                                    );
                                    return request;
                                })
                                .header(
                                        "X-Request-Id",
                                        "REQ-008"
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
                .andExpect(
                        status().isInternalServerError()
                )
                .andExpect(
                        header().string(
                                "X-Request-Id",
                                "REQ-008"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.referenceId"
                        ).value("REQ-008")
                )
                .andExpect(
                        jsonPath(
                                "$.responseCode"
                        ).value("99")
                )
                .andExpect(
                        jsonPath(
                                "$.responseDesc"
                        ).value(
                                "Internal server error"
                        )
                );
    }
}