package com.wizz.card_management.controller;

import com.wizz.card_management.dto.request.SetCardStatusRequest;
import com.wizz.card_management.dto.response.SetCardStatusResponse;
import com.wizz.card_management.ratelimit.RateLimitService;
import com.wizz.card_management.service.CardStatusUpdateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/cards")
@Tag(
        name = "Card Status API",
        description = "Card status update API"
)
public class CardStatusController {

    private final CardStatusUpdateService cardStatusUpdateService;
    private final RateLimitService rateLimitService;

    public CardStatusController(
            CardStatusUpdateService cardStatusUpdateService,
            RateLimitService rateLimitService) {

        this.cardStatusUpdateService = cardStatusUpdateService;
        this.rateLimitService = rateLimitService;
    }

    @Operation(
            summary = "Update card status",
            description = "Updates the status of an existing card"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Card status updated successfully"
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
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @PostMapping("/setStatus")
    public ResponseEntity<SetCardStatusResponse> updateCardStatus(

            @Valid
            @RequestBody
            SetCardStatusRequest request,

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
                    .header("Retry-After", "60")
                    .build();
        }

        SetCardStatusResponse response =
                cardStatusUpdateService.updateCardStatus(
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