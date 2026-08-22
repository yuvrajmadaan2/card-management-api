package com.wizz.card_management.controller;

import com.wizz.card_management.dto.request.CardDetailsRequest;
import com.wizz.card_management.dto.response.CardDetailsResponse;
import com.wizz.card_management.service.CardDetailsReadService;
import com.wizz.card_management.ratelimit.RateLimitService;

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
        name = "Card Details API",
        description = "Card details retrieval API"
)
public class CardDetailsController {

    private final CardDetailsReadService cardDetailsReadService;
    private final RateLimitService rateLimitService;

    public CardDetailsController(
            CardDetailsReadService cardDetailsReadService,
            RateLimitService rateLimitService) {

        this.cardDetailsReadService = cardDetailsReadService;
        this.rateLimitService = rateLimitService;
    }

    @Operation(
            summary = "Get card details",
            description = "Retrieves details for one or more cards"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Card details retrieved successfully"
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
            )
    })
    @PostMapping("/details")
    public ResponseEntity<CardDetailsResponse> getCardDetails(

            @Valid
            @RequestBody
            CardDetailsRequest request,

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

        CardDetailsResponse response =
                cardDetailsReadService.getCardDetails(
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