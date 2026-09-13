package com.wizz.card_management.service.impl;

import com.wizz.card_management.dto.request.CreateCardRequest;
import com.wizz.card_management.service.CardIssuanceService;
import com.wizz.card_management.service.IssuanceResult;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CardIssuanceServiceImpl implements CardIssuanceService {

    @Override
    public IssuanceResult issueCard(CreateCardRequest request) {

        /*
         * Development issuer adapter.
         *
         * The real card processor/issuer integration should replace
         * this implementation once processor API details,
         * credentials and endpoint contracts are provided.
         *
         * CardCreateService does not generate PAN or expiry directly.
         */

        String processorReference =
                "DEV-" + UUID.randomUUID();

        String maskedCardNumber =
                "4111XXXXXXXX1111";

        String expiryDate =
                "07/2031";

        return new IssuanceResult(
                processorReference,
                maskedCardNumber,
                expiryDate
        );
    }
}