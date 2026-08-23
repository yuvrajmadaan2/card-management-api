package com.wizz.card_management.controller;

import com.wizz.card_management.dto.request.TxnControlsFetchRequest;
import com.wizz.card_management.dto.response.TxnControlsFetchResponse;
import com.wizz.card_management.ratelimit.RateLimitService;
import com.wizz.card_management.service.CardControlsReadService;

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
        name = "Transaction Controls API",
        description = "Transaction channel controls retrieval API"
)
public class TxnControlsController {

    private final CardControlsReadService cardControlsReadService;
    private final RateLimitService rateLimitService;

    public TxnControlsController(
            CardControlsReadService cardControlsReadService,
            RateLimitService rateLimitService) {

        this.cardControlsReadService = cardControlsReadService;
        this.rateLimitService = rateLimitService;
    }

    @Operation(
            summary = "Get transaction controls",
            description =
                    "Retrieves transaction channel controls for one or more cards"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description =
                            "Transaction controls retrieved successfully"
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
    @PostMapping("/txnControls")
    public ResponseEntity<TxnControlsFetchResponse> getTransactionControls(

            @Valid
            @RequestBody
            TxnControlsFetchRequest request,

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

        TxnControlsFetchResponse response =
                cardControlsReadService.getTransactionControls(
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