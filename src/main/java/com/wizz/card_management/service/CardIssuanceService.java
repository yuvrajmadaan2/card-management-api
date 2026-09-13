package com.wizz.card_management.service;

import com.wizz.card_management.dto.request.CreateCardRequest;

public interface CardIssuanceService {

    IssuanceResult issueCard(CreateCardRequest request);
}