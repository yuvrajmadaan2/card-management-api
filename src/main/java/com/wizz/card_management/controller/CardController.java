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

@RestController
@RequestMapping("/cards")
@Tag(
        name = "Card API",
        description = "Card management APIs"
)
public class CardController {

    private final CardCreateService cardCreateService;

    public CardController(CardCreateService cardCreateService) {
        this.cardCreateService = cardCreateService;
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

            @Parameter(description = "Bearer access token")
            @RequestHeader("Authorization")
            String authorization,

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
            String channel) {

        try {

            CreateCardResponse response =
                    cardCreateService.createCard(
                            request,
                            requestId,
                            idempotencyKey,
                            channel
                    );

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {

            if ("IDEMPOTENCY_CONFLICT".equals(e.getMessage())) {

                CreateCardResponse response =
                        new CreateCardResponse();

                response.setReferenceId(requestId);
                response.setResponseCode("409");
                response.setResponseDesc(
                        "Idempotency key conflict"
                );

                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(response);
            }

            throw e;
        }
    }
}