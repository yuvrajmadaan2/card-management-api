package com.wizz.card_management.controller;

import com.wizz.card_management.dto.request.TxnControlsSetRequest;
import com.wizz.card_management.dto.response.TxnControlsSetResponse;
import com.wizz.card_management.ratelimit.RateLimitService;
import com.wizz.card_management.service.TxnControlsSetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@Tag(
        name = "Transaction Controls Set API",
        description = "Transaction channel control update API"
)
public class TxnControlsSetController {

    private final TxnControlsSetService txnControlsSetService;
    private final RateLimitService rateLimitService;

    public TxnControlsSetController(
            TxnControlsSetService txnControlsSetService,
            RateLimitService rateLimitService) {

        this.txnControlsSetService = txnControlsSetService;
        this.rateLimitService = rateLimitService;
    }

    @Operation(
            summary = "Set transaction control",
            description =
                    "Updates the allowed state of a transaction channel for a card"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction control processed"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @PostMapping("/txnControls/set")
    public ResponseEntity<TxnControlsSetResponse>
    setTransactionControl(

            @Valid
            @RequestBody
            TxnControlsSetRequest request,

            @RequestHeader("X-Request-Id")
            String requestId,

            @RequestHeader(
                    value = "X-Channel",
                    required = false
            )
            String channel,

            JwtAuthenticationToken authentication) {

        String partnerId =
                authentication
                        .getToken()
                        .getSubject();

        if (!rateLimitService.isAllowed(partnerId)) {

            return ResponseEntity
                    .status(429)
                    .header("X-Request-Id", requestId)
                    .build();
        }

        TxnControlsSetResponse response =
                txnControlsSetService.setTransactionControl(
                        request,
                        requestId,
                        channel,
                        partnerId
                );

        return ResponseEntity
                .ok()
                .header("X-Request-Id", requestId)
                .body(response);
    }
}