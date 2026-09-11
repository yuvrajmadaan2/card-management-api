package com.wizz.card_management.controller;

import com.wizz.card_management.dto.response.TxnControlsFetchResponse;
import com.wizz.card_management.ratelimit.RateLimitService;
import com.wizz.card_management.service.CardControlsReadService;

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

@WebMvcTest(TxnControlsController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
        OAuth2ResourceServerAutoConfiguration.class,
        OAuth2ResourceServerWebSecurityAutoConfiguration.class
})
class TxnControlsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardControlsReadService cardControlsReadService;

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
                                "SCOPE_cards:read"
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
                  ],
                  "customerId": "CUSTOMER001"
                }
                """;
    }

    private TxnControlsFetchResponse successResponse() {

        TxnControlsFetchResponse response =
                new TxnControlsFetchResponse();

        response.setReferenceId("REQ-001");
        response.setResponseCode("00");
        response.setResponseDesc(
                "Transaction channel controls retrieved successfully"
        );

        TxnControlsFetchResponse.CardControls cardControls =
                new TxnControlsFetchResponse.CardControls();

        cardControls.setCardId("CARD001");

        List<TxnControlsFetchResponse.ControlRow> rows =
                List.of(
                        createRow("ATM", true, true),
                        createRow("POS", true, true),
                        createRow("ECOM", true, true),
                        createRow("NFC", true, true),
                        createRow("MAG", false, true),
                        createRow("DOM", false, false),
                        createRow("INT", true, true)
                );

        cardControls.setLists(rows);

        response.setChannels(
                List.of(cardControls)
        );

        return response;
    }

    private TxnControlsFetchResponse.ControlRow createRow(
            String channelType,
            boolean allowed,
            boolean editable) {

        TxnControlsFetchResponse.ControlRow row =
                new TxnControlsFetchResponse.ControlRow();

        row.setChannelType(channelType);
        row.setAllowed(allowed);
        row.setEditable(editable);

        return row;
    }

    @Test
    void validRequest_shouldReturn200() throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardControlsReadService.getTransactionControls(
                any(),
                eq("REQ-001"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(successResponse());

        mockMvc.perform(
                        post("/v1/txnControls")
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
                                "$.referenceId"
                        ).value("REQ-001")
                )
                .andExpect(
                        jsonPath(
                                "$.responseCode"
                        ).value("00")
                )
                .andExpect(
                        jsonPath(
                                "$.responseDesc"
                        ).value(
                                "Transaction channel controls retrieved successfully"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.channels[0].cardId"
                        ).value("CARD001")
                )
                .andExpect(
                        jsonPath(
                                "$.channels[0].lists.length()"
                        ).value(7)
                );

        verify(cardControlsReadService)
                .getTransactionControls(
                        any(),
                        eq("REQ-001"),
                        eq("WEB"),
                        eq("partner-001")
                );
    }

    @Test
    void unknownCard_shouldReturnBusinessDecline()
            throws Exception {

        TxnControlsFetchResponse response =
                new TxnControlsFetchResponse();

        response.setReferenceId("REQ-002");
        response.setResponseCode("10");
        response.setResponseDesc(
                "No transaction controls found"
        );
        response.setChannels(List.of());

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardControlsReadService.getTransactionControls(
                any(),
                eq("REQ-002"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);

        mockMvc.perform(
                        post("/v1/txnControls")
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
                                "No transaction controls found"
                        )
                );
    }

    @Test
    void ownershipMismatch_shouldReturnBusinessDecline()
            throws Exception {

        TxnControlsFetchResponse response =
                new TxnControlsFetchResponse();

        response.setReferenceId("REQ-003");
        response.setResponseCode("90");
        response.setResponseDesc(
                "Customer-card ownership mismatch"
        );
        response.setChannels(List.of());

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardControlsReadService.getTransactionControls(
                any(),
                eq("REQ-003"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);

        mockMvc.perform(
                        post("/v1/txnControls")
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
                                .content(validJson())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.responseCode"
                        ).value("90")
                )
                .andExpect(
                        jsonPath(
                                "$.responseDesc"
                        ).value(
                                "Customer-card ownership mismatch"
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
                        post("/v1/txnControls")
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
                                .content(invalidJson)
                )
                .andExpect(
                        status().isBadRequest()
                );

        verifyNoInteractions(
                cardControlsReadService
        );
    }

    @Test
    void blankCardId_shouldReturn400()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        String invalidJson = """
                {
                  "cards": [
                    {
                      "cardId": ""
                    }
                  ]
                }
                """;

        mockMvc.perform(
                        post("/v1/txnControls")
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
                cardControlsReadService
        );
    }

    @Test
    void rateLimitExceeded_shouldReturn429()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(false);

        mockMvc.perform(
                        post("/v1/txnControls")
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
                cardControlsReadService
        );
    }

    @Test
    void missingRequestId_shouldReturn400()
            throws Exception {

        mockMvc.perform(
                        post("/v1/txnControls")
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
                cardControlsReadService
        );
    }

    @Test
    void internalException_shouldReturn500()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(cardControlsReadService.getTransactionControls(
                any(),
                eq("REQ-007"),
                eq("WEB"),
                eq("partner-001")
        )).thenThrow(
                new RuntimeException(
                        "Database unavailable"
                )
        );

        mockMvc.perform(
                        post("/v1/txnControls")
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
                .andExpect(
                        status().isInternalServerError()
                )
                .andExpect(
                        header().string(
                                "X-Request-Id",
                                "REQ-007"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.referenceId"
                        ).value("REQ-007")
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