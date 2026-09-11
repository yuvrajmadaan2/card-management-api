package com.wizz.card_management.service.impl;

import com.wizz.card_management.dto.request.SetCardStatusRequest;
import com.wizz.card_management.dto.response.SetCardStatusResponse;
import com.wizz.card_management.entity.Card;
import com.wizz.card_management.entity.CardProgram;
import com.wizz.card_management.repository.CardProgramRepository;
import com.wizz.card_management.repository.CardRepository;
import com.wizz.card_management.service.CardStatusUpdateService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class CardStatusUpdateServiceImpl
        implements CardStatusUpdateService {

    private static final Logger log =
            LoggerFactory.getLogger(CardStatusUpdateServiceImpl.class);

    private static final Set<String> VALID_STATUS_CODES =
            Set.of("A", "B", "S", "R", "I");

    private final CardRepository cardRepository;
    private final CardProgramRepository cardProgramRepository;

    public CardStatusUpdateServiceImpl(
            CardRepository cardRepository,
            CardProgramRepository cardProgramRepository) {

        this.cardRepository = cardRepository;
        this.cardProgramRepository = cardProgramRepository;
    }

    @Override
    @Transactional
    public SetCardStatusResponse updateCardStatus(
            SetCardStatusRequest request,
            String requestId,
            String channel,
            String partnerId) {

        log.info(
                "Updating card status requestId={} partnerId={} channel={}",
                requestId,
                partnerId,
                channel
        );

        SetCardStatusResponse response =
                new SetCardStatusResponse();

        response.setReferenceId(requestId);

        String cardId =
                request.getCard().getCardId();

        String requestedStatus =
                request.getCard().getStatusCode();

        String reasonCode =
                request.getCard().getReasonCode();

        /*
         * Validate status code.
         */
        if (!VALID_STATUS_CODES.contains(requestedStatus)) {

                log.warn(
                        "Invalid card status requestId={} cardId={} requestedStatus={}",
                        requestId,
                        cardId,
                        requestedStatus
                );
            response.setResponseCode("01");
            response.setResponseDesc(
                    "Invalid card status code"
            );

            return response;
        }

        /*
         * reasonCode is mandatory for B, S and R.
         */
        if (requiresReasonCode(requestedStatus)
                && (reasonCode == null
                || reasonCode.isBlank())) {
                log.warn(
                        "Missing reason code requestId={} cardId={} requestedStatus={}",
                        requestId,
                        cardId,
                        requestedStatus
                );
            response.setResponseCode("02");
            response.setResponseDesc(
                    "Reason code is mandatory for the requested card status"
            );

            return response;
        }

        /*
         * Find card.
         */
        Optional<Card> cardOptional =
                cardRepository.findByCardId(cardId);

        if (cardOptional.isEmpty()) {

            log.warn(
                    "Card not found requestId={} cardId={}",
                    requestId,
                    cardId
            );

            response.setResponseCode("10");
            response.setResponseDesc(
                    "Card not found"
            );

            return response;
        }

        Card card = cardOptional.get();

        String currentStatus =
                card.getCardStatus();

        /*
         * Same status.
         */
        if (requestedStatus.equals(currentStatus)) {

            response.setResponseCode("00");
            response.setResponseDesc(
                    "Card status is already "
                            + getCardStatusDescription(
                            requestedStatus
                    )
            );

            populateCardDetails(response, card);

            return response;
        }

        /*
         * Validate the supported transition rules.
         *
         * The internship guide requires transition validation,
         * but does not provide a complete transition matrix.
         * Therefore only terminal-state protection is enforced
         * here without inventing undocumented transitions.
         */
        if (isTerminalStatus(currentStatus)) {
                log.warn(
                        "Invalid card status transition requestId={} cardId={} currentStatus={} requestedStatus={}",
                        requestId,
                        cardId,
                        currentStatus,
                        requestedStatus
                );
            response.setResponseCode("31");
            response.setResponseDesc(
                    "Invalid card status transition"
            );

            return response;
        }

        /*
         * Update status.
         */
        card.setCardStatus(requestedStatus);

        cardRepository.save(card);

        log.info(
                "Card status updated successfully requestId={} cardId={} oldStatus={} newStatus={}",
                requestId,
                cardId,
                currentStatus,
                requestedStatus
        );

        populateCardDetails(response, card);

        response.setResponseCode("00");
        response.setResponseDesc(
                "Card status updated to "
                        + getCardStatusDescription(
                        requestedStatus
                )
        );

        return response;
    }

    private boolean requiresReasonCode(
            String statusCode) {

        return "B".equals(statusCode)
                || "S".equals(statusCode)
                || "R".equals(statusCode);
    }

    private boolean isTerminalStatus(
            String statusCode) {

        /*
         * R = REPLACED is treated as terminal.
         *
         * No further transition is allowed from a
         * replaced card.
         */
        return "R".equals(statusCode);
    }

    private void populateCardDetails(
            SetCardStatusResponse response,
            Card card) {

        response.setCardNumber(
                card.getCardNumber()
        );

        response.setCustomerId(
                card.getCustomerId()
        );

        Optional<CardProgram> programOptional =
                cardProgramRepository.findByProgramId(
                        card.getCardProgramId()
                );

        programOptional.ifPresent(program ->
                response.setCardProgramName(
                        program.getProgramName()
                )
        );
    }

    private String getCardStatusDescription(
            String statusCode) {

        switch (statusCode) {

            case "A":
                return "ACTIVE";

            case "B":
                return "BLOCKED";

            case "S":
                return "TEMP SUSPENDED";

            case "R":
                return "REPLACED";

            case "I":
                return "INACTIVE";

            default:
                return "UNKNOWN";
        }
    }
}