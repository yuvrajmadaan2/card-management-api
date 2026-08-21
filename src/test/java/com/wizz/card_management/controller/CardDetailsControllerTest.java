package com.wizz.card_management.controller;

import com.wizz.card_management.dto.response.CardDetailsResponse;
import com.wizz.card_management.ratelimit.RateLimitService;
import com.wizz.card_management.service.CardDetailsReadService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;


@WebMvcTest(CardDetailsController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
        OAuth2ResourceServerAutoConfiguration.class,
        OAuth2ResourceServerWebSecurityAutoConfiguration.class
})
class CardDetailsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardDetailsReadService cardDetailsReadService;

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
                  "cards": [
                    {
                      "cardId": "CARD001"
                    }
                  ]
                }
                """;
    }


    private String validJsonWithCustomerId() {

        return """
                {
                  "cards": [
                    {
                      "cardId": "CARD001"
                    }
                  ],
                  "customerId": "0012342"
                }
                """;
    }


    @Test
    void validRequest_shouldReturn200() throws Exception {

        CardDetailsResponse response =
                new CardDetailsResponse();

        response.setReferenceId("REQ-001");
        response.setResponseCode("00");
        response.setResponseDesc(
                "Card details retrieved successfully"
        );

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardDetailsReadService.getCardDetails(
                any(),
                eq("REQ-001"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);


        mockMvc.perform(
                        post("/cards/details")
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
                );

        verify(cardDetailsReadService).getCardDetails(
                any(),
                eq("REQ-001"),
                eq("WEB"),
                eq("partner-001")
        );
    }


    @Test
    void cardNotFound_shouldReturn200WithBusinessDecline()
            throws Exception {

        CardDetailsResponse response =
                new CardDetailsResponse();

        response.setReferenceId("REQ-002");
        response.setResponseCode("10");
        response.setResponseDesc(
                "No card details found"
        );
        response.setCards(List.of());

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardDetailsReadService.getCardDetails(
                any(),
                eq("REQ-002"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);


        mockMvc.perform(
                        post("/cards/details")
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
                        ).value(
                                "No card details found"
                        )
                );
    }


    @Test
    void invalidRequest_shouldReturn400()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        String invalidJson = """
                {
                  "cards": []
                }
                """;


        mockMvc.perform(
                        post("/cards/details")
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
                .andExpect(
                        status().isBadRequest()
                );

        verifyNoInteractions(
                cardDetailsReadService
        );
    }


    @Test
    void rateLimitExceeded_shouldReturn429()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(false);


        mockMvc.perform(
                        post("/cards/details")
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
                .andExpect(
                        status().isTooManyRequests()
                )
                .andExpect(
                        header().string(
                                "X-Request-Id",
                                "REQ-004"
                        )
                );

        verifyNoInteractions(
                cardDetailsReadService
        );
    }


    @Test
    void missingRequestId_shouldReturn400()
            throws Exception {

        mockMvc.perform(
                        post("/cards/details")
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
                cardDetailsReadService
        );
    }


    @Test
    void ownershipMismatch_shouldReturn200WithBusinessDecline()
            throws Exception {

        CardDetailsResponse response =
                new CardDetailsResponse();

        response.setReferenceId("REQ-005");
        response.setResponseCode("90");
        response.setResponseDesc(
                "Customer–card ownership mismatch — details request declined"
        );
        response.setCards(List.of());

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardDetailsReadService.getCardDetails(
                any(),
                eq("REQ-005"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);


        mockMvc.perform(
                        post("/cards/details")
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
                                .content(
                                        validJsonWithCustomerId()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                "X-Request-Id",
                                "REQ-005"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.responseCode"
                        ).value("90")
                )
                .andExpect(
                        jsonPath(
                                "$.responseDesc"
                        ).value(
                                "Customer–card ownership mismatch — details request declined"
                        )
                );
    }


    @Test
    void partialSuccess_shouldReturn200WithFoundCards()
            throws Exception {

        CardDetailsResponse response =
                new CardDetailsResponse();

        response.setReferenceId("REQ-006");
        response.setResponseCode("00");
        response.setResponseDesc(
                "Card details retrieved successfully"
        );

        CardDetailsResponse.CardDetail detail =
                new CardDetailsResponse.CardDetail();

        detail.setCardId("CARD001");
        detail.setCustomerId("0012342");
        detail.setNameOnCard("Chuck Yeager");
        detail.setIssuedDate("2026-07-07");

        response.setCards(List.of(detail));

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardDetailsReadService.getCardDetails(
                any(),
                eq("REQ-006"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);


        mockMvc.perform(
                        post("/cards/details")
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
                                .content(
                                        validJsonWithCustomerId()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.responseCode"
                        ).value("00")
                )
                .andExpect(
                        jsonPath(
                                "$.cards[0].cardId"
                        ).value("CARD001")
                )
                .andExpect(
                        jsonPath(
                                "$.cards[0].customerId"
                        ).value("0012342")
                );
    }
}