package com.wizz.card_management.service;

import com.wizz.card_management.dto.request.CreateCardRequest;
import com.wizz.card_management.dto.response.CreateCardResponse;

public interface CardCreateService {

    CreateCardResponse createCard(
            CreateCardRequest request,
            String requestId,
            String idempotencyKey,
            String channel
    );
}