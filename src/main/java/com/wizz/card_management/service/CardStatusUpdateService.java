package com.wizz.card_management.service;

import com.wizz.card_management.dto.request.SetCardStatusRequest;
import com.wizz.card_management.dto.response.SetCardStatusResponse;

public interface CardStatusUpdateService {

    SetCardStatusResponse updateCardStatus(
            SetCardStatusRequest request,
            String requestId,
            String channel,
            String partnerId
    );
}