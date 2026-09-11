package com.wizz.card_management.service;

import com.wizz.card_management.dto.request.CardDetailsRequest;
import com.wizz.card_management.dto.response.CardDetailsResponse;

public interface CardDetailsReadService {

    CardDetailsResponse getCardDetails(
            CardDetailsRequest request,
            String requestId,
            String channel,
            String partnerId
    );
}