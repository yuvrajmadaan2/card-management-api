package com.wizz.card_management.service;

import com.wizz.card_management.dto.request.TxnControlsSetRequest;
import com.wizz.card_management.dto.response.TxnControlsSetResponse;

public interface TxnControlsSetService {

    TxnControlsSetResponse setTransactionControl(
            TxnControlsSetRequest request,
            String requestId,
            String channel,
            String partnerId
    );
}