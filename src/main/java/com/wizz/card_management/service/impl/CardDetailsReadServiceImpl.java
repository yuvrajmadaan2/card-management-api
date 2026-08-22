package com.wizz.card_management.service.impl;

import com.wizz.card_management.dto.request.CardDetailsRequest;
import com.wizz.card_management.dto.response.CardDetailsResponse;
import com.wizz.card_management.entity.Card;
import com.wizz.card_management.entity.CardProgram;
import com.wizz.card_management.repository.CardProgramRepository;
import com.wizz.card_management.repository.CardRepository;
import com.wizz.card_management.service.CardDetailsReadService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CardDetailsReadServiceImpl
        implements CardDetailsReadService {

    private static final Logger log =
            LoggerFactory.getLogger(CardDetailsReadServiceImpl.class);

    private final CardRepository cardRepository;
    private final CardProgramRepository cardProgramRepository;

    public CardDetailsReadServiceImpl(
            CardRepository cardRepository,
            CardProgramRepository cardProgramRepository) {

        this.cardRepository = cardRepository;
        this.cardProgramRepository = cardProgramRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CardDetailsResponse getCardDetails(
            CardDetailsRequest request,
            String requestId,
            String channel,
            String partnerId) {

        log.info(
                "Reading card details requestId={} partnerId={} channel={}",
                requestId,
                partnerId,
                channel
        );

        CardDetailsResponse response =
                new CardDetailsResponse();

        response.setReferenceId(requestId);

        List<CardDetailsResponse.CardDetail> cardDetails =
                new ArrayList<>();

        Set<String> uniqueCardIds = new HashSet<>();

        String requestedCustomerId =
                request.getCustomerId();

        for (CardDetailsRequest.CardRef cardRef :
                request.getCards()) {

            String cardId = cardRef.getCardId();

            // De-duplicate card IDs
            if (!uniqueCardIds.add(cardId)) {
                continue;
            }

            Optional<Card> cardOptional =
                    cardRepository.findByCardId(cardId);

            // Card does not exist
            if (cardOptional.isEmpty()) {

                log.info(
                        "Card not found requestId={} cardId={}",
                        requestId,
                        cardId
                );

                continue;
            }

            Card card = cardOptional.get();

            /*
             * Ownership check.
             *
             * customerId is conditional according to the API
             * specification, so we only perform this check when
             * customerId is supplied in the request.
             */
            if (requestedCustomerId != null
                    && !requestedCustomerId.isBlank()
                    && !requestedCustomerId.equals(
                            card.getCustomerId())) {

                log.info(
                        "Customer-card ownership mismatch requestId={} cardId={}",
                        requestId,
                        cardId
                );

                response.setResponseCode("90");
                response.setResponseDesc(
                        "Customer–card ownership mismatch — details request declined"
                );
                response.setCards(new ArrayList<>());

                return response;
            }

            CardDetailsResponse.CardDetail detail =
                    new CardDetailsResponse.CardDetail();

            detail.setCardId(
                    card.getCardId()
            );

            detail.setCardProgramType(
                    card.getCardProgramType()
            );

            detail.setCardType(
                    card.getCardType()
            );

            detail.setCardProgramId(
                    card.getCardProgramId()
            );

            detail.setCardNumber(
                    card.getCardNumber()
            );

            detail.setExpiryDate(
                    card.getExpiryDate()
            );

            detail.setCardStatus(
                    card.getCardStatus()
            );

            detail.setCardStatusDesc(
                    getCardStatusDescription(
                            card.getCardStatus()
                    )
            );

            detail.setNameOnCard(
                    card.getNameOnCard()
            );

            detail.setCustomerId(
                    card.getCustomerId()
            );

            detail.setIssuedDate(
                    card.getIssuedDate()
            );

            Optional<CardProgram> programOptional =
                    cardProgramRepository.findByProgramId(
                            card.getCardProgramId()
                    );

            programOptional.ifPresent(program ->
                    detail.setCardProgramName(
                            program.getProgramName()
                    )
            );

            cardDetails.add(detail);
        }

        response.setCards(cardDetails);

        /*
         * No requested cards could be resolved.
         */
        if (cardDetails.isEmpty()) {

            response.setResponseCode("10");
            response.setResponseDesc(
                    "No card details found"
            );

            return response;
        }

        /*
         * At least one card was found.
         * This also supports partial success when some
         * requested card IDs are unknown.
         */
        response.setResponseCode("00");
        response.setResponseDesc(
                "Card details retrieved successfully"
        );

        return response;
    }

    private String getCardStatusDescription(
            String cardStatus) {

        if ("A".equals(cardStatus)) {
            return "ACTIVE";
        }

        if ("I".equals(cardStatus)) {
            return "INACTIVE";
        }

        return "UNKNOWN";
    }
}