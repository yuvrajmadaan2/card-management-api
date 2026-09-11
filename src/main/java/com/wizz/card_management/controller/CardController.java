package com.wizz.card_management.controller;

import com.wizz.card_management.dto.request.CreateCardRequest;
import com.wizz.card_management.dto.response.CreateCardResponse;
import com.wizz.card_management.service.CardCreateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.wizz.card_management.ratelimit.RateLimitService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/v1/cards")
@Tag(
        name = "Card API",
        description = "Card management APIs"
)
public class CardController {

    private final CardCreateService cardCreateService;
    private final RateLimitService rateLimitService;

        public CardController(
                CardCreateService cardCreateService,
                RateLimitService rateLimitService) {

        this.cardCreateService = cardCreateService;
        this.rateLimitService = rateLimitService;
        }

    @Operation(
            summary = "Create card",
            description = "Creates a new card under a card program"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Card created successfully or business decline"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Idempotency key conflict"
            )
    })
    @PostMapping
    public ResponseEntity<CreateCardResponse> createCard(

            @Valid
            @RequestBody
            CreateCardRequest request,



            @Parameter(description = "Unique request ID")
            @RequestHeader("X-Request-Id")
            String requestId,

            @Parameter(description = "Unique idempotency key")
            @RequestHeader("X-Idempotency-Key")
            String idempotencyKey,

            @Parameter(description = "Request channel")
            @RequestHeader(
                    value = "X-Channel",
                    required = false
            )
            String channel,
            JwtAuthenticationToken authentication) {
        String partnerId = authentication
                .getToken()
                .getSubject();
        if (!rateLimitService.isAllowed(partnerId)) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-Request-Id", requestId)
                .header("Retry-After", "60")
                .build();
        }                

        CreateCardResponse response =
                cardCreateService.createCard(
                        request,
                        requestId,
                        idempotencyKey,
                        channel,
                        partnerId
                );

        return ResponseEntity
                .ok()
                .header("X-Request-Id", requestId)
                .body(response);
    }
}