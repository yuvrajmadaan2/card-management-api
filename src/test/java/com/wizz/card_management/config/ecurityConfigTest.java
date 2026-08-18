package com.wizz.card_management.config;

import com.wizz.card_management.controller.CardController;
import com.wizz.card_management.ratelimit.RateLimitService;
import com.wizz.card_management.service.CardCreateService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest(
        properties = {
                "spring.sql.init.mode=never"
        }
)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardCreateService cardCreateService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    void missingWriteScope_shouldReturn403() throws Exception {

        mockMvc.perform(
                post("/v1/cards")
                        .with(
                                jwt().authorities(
                                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                "SCOPE_cards:read"
                                        )
                                )
                        )
                        .header(
                                "X-Request-Id",
                                "REQ-SEC-001"
                        )
                        .header(
                                "X-Idempotency-Key",
                                "KEY-SEC-001"
                        )
                        .contentType(
                                "application/json"
                        )
                        .content("""
                                {
                                  "card": {
                                    "cardProgramType": "D",
                                    "cardType": "V",
                                    "cardProgramId": "PROGRAM-001"
                                  }
                                }
                                """)
        )
        .andExpect(
                status().isForbidden()
        )
        .andExpect(
                header().string(
                        "X-Request-Id",
                        "REQ-SEC-001"
                )
        );
    }
}