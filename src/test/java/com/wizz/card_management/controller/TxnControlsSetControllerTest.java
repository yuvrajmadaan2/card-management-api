package com.wizz.card_management.controller;

import com.wizz.card_management.dto.response.TxnControlsSetResponse;
import com.wizz.card_management.ratelimit.RateLimitService;
import com.wizz.card_management.service.TxnControlsSetService;

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

@WebMvcTest(TxnControlsSetController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
        OAuth2ResourceServerAutoConfiguration.class,
        OAuth2ResourceServerWebSecurityAutoConfiguration.class
})
class TxnControlsSetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TxnControlsSetService txnControlsSetService;

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
                  "cardId": "CARD001",
                  "customerId": "CUSTOMER001",
                  "channel": {
                    "channelType": "ATM",
                    "allowed": true
                  }
                }
                """;
    }

    private TxnControlsSetResponse successResponse() {

        TxnControlsSetResponse response =
                new TxnControlsSetResponse();

        response.setReferenceId("REQ-001");
        response.setCardId("CARD001");
        response.setResponseCode("00");
        response.setResponseDesc(
                "Transaction control updated successfully"
        );

        return response;
    }

    @Test
    void validRequest_shouldReturn200() throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(txnControlsSetService.setTransactionControl(
                any(),
                eq("REQ-001"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(successResponse());

        mockMvc.perform(
                        post("/v1/txnControls/set")
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
                                "$.cardId"
                        ).value("CARD001")
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
                                "Transaction control updated successfully"
                        )
                );

        verify(txnControlsSetService)
                .setTransactionControl(
                        any(),
                        eq("REQ-001"),
                        eq("WEB"),
                        eq("partner-001")
                );
    }

    @Test
    void invalidChannel_shouldReturnBusinessResponse()
            throws Exception {

        TxnControlsSetResponse response =
                new TxnControlsSetResponse();

        response.setReferenceId("REQ-002");
        response.setCardId("CARD001");
        response.setResponseCode("01");
        response.setResponseDesc(
                "Invalid transaction channel type"
        );

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(txnControlsSetService.setTransactionControl(
                any(),
                eq("REQ-002"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);

        String json = """
                {
                  "cardId": "CARD001",
                  "customerId": "CUSTOMER001",
                  "channel": {
                    "channelType": "INVALID",
                    "allowed": true
                  }
                }
                """;

        mockMvc.perform(
                        post("/v1/txnControls/set")
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
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.responseCode"
                        ).value("01")
                );
    }

    @Test
    void unknownCard_shouldReturnBusinessResponse()
            throws Exception {

        TxnControlsSetResponse response =
                new TxnControlsSetResponse();

        response.setReferenceId("REQ-003");
        response.setCardId("UNKNOWN");
        response.setResponseCode("10");
        response.setResponseDesc(
                "Card not found"
        );

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(txnControlsSetService.setTransactionControl(
                any(),
                eq("REQ-003"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);

        String json = """
                {
                  "cardId": "UNKNOWN",
                  "customerId": "CUSTOMER001",
                  "channel": {
                    "channelType": "ATM",
                    "allowed": true
                  }
                }
                """;

        mockMvc.perform(
                        post("/v1/txnControls/set")
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
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.responseCode"
                        ).value("10")
                );
    }

    @Test
    void ownershipMismatch_shouldReturnBusinessResponse()
            throws Exception {

        TxnControlsSetResponse response =
                new TxnControlsSetResponse();

        response.setReferenceId("REQ-004");
        response.setCardId("CARD001");
        response.setResponseCode("90");
        response.setResponseDesc(
                "Customer-card ownership mismatch"
        );

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(txnControlsSetService.setTransactionControl(
                any(),
                eq("REQ-004"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);

        mockMvc.perform(
                        post("/v1/txnControls/set")
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
                        ).value("90")
                );
    }

    @Test
    void nonEditableChannel_shouldReturnBusinessResponse()
            throws Exception {

        TxnControlsSetResponse response =
                new TxnControlsSetResponse();

        response.setReferenceId("REQ-005");
        response.setCardId("CARD001");
        response.setResponseCode("31");
        response.setResponseDesc(
                "Transaction channel is not editable"
        );

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(txnControlsSetService.setTransactionControl(
                any(),
                eq("REQ-005"),
                eq("WEB"),
                eq("partner-001")
        )).thenReturn(response);

        String json = """
                {
                  "cardId": "CARD001",
                  "customerId": "CUSTOMER001",
                  "channel": {
                    "channelType": "DOM",
                    "allowed": true
                  }
                }
                """;

        mockMvc.perform(
                        post("/v1/txnControls/set")
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
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.responseCode"
                        ).value("31")
                );
    }

    @Test
    void invalidRequest_shouldReturn400()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        String invalidJson = """
                {
                  "cardId": "",
                  "channel": {
                    "channelType": "ATM",
                    "allowed": true
                  }
                }
                """;

        mockMvc.perform(
                        post("/v1/txnControls/set")
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
                                .content(invalidJson)
                )
                .andExpect(
                        status().isBadRequest()
                );

        verifyNoInteractions(
                txnControlsSetService
        );
    }

    @Test
    void missingRequestId_shouldReturn400()
            throws Exception {

        mockMvc.perform(
                        post("/v1/txnControls/set")
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
                txnControlsSetService
        );
    }

    @Test
    void rateLimitExceeded_shouldReturn429()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(false);

        mockMvc.perform(
                        post("/v1/txnControls/set")
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
                        status().isTooManyRequests()
                )
                .andExpect(
                        header().string(
                                "X-Request-Id",
                                "REQ-007"
                        )
                );

        verifyNoInteractions(
                txnControlsSetService
        );
    }

    @Test
    void internalException_shouldReturn500()
            throws Exception {

        when(rateLimitService.isAllowed("partner-001"))
                .thenReturn(true);

        when(txnControlsSetService.setTransactionControl(
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
                        post("/v1/txnControls/set")
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