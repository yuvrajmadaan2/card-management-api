package com.wizz.card_management.service;

import com.wizz.card_management.dto.request.TxnControlsFetchRequest;
import com.wizz.card_management.dto.response.TxnControlsFetchResponse;

public interface CardControlsReadService {

    TxnControlsFetchResponse getTransactionControls(
            TxnControlsFetchRequest request,
            String requestId,
            String channel,
            String partnerId
    );
}