package com.wizz.card_management.service.impl;

import com.wizz.card_management.dto.request.TxnControlsFetchRequest;
import com.wizz.card_management.dto.response.TxnControlsFetchResponse;
import com.wizz.card_management.entity.Card;
import com.wizz.card_management.entity.CardProgram;
import com.wizz.card_management.repository.CardProgramRepository;
import com.wizz.card_management.repository.CardRepository;
import com.wizz.card_management.service.CardControlsReadService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.wizz.card_management.entity.TransactionControl;
import com.wizz.card_management.repository.TransactionControlRepository;



@Service
public class CardControlsReadServiceImpl
        implements CardControlsReadService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CardControlsReadServiceImpl.class
            );

    private static final List<String> CHANNEL_TYPES =
            List.of(
                    "ATM",
                    "POS",
                    "ECOM",
                    "NFC",
                    "MAG",
                    "DOM",
                    "INT"
            );

    private final CardRepository cardRepository;
    private final CardProgramRepository cardProgramRepository;
    private final TransactionControlRepository transactionControlRepository;

    public CardControlsReadServiceImpl(
            CardRepository cardRepository,
            CardProgramRepository cardProgramRepository,
            TransactionControlRepository transactionControlRepository) {

        this.cardRepository = cardRepository;
        this.cardProgramRepository = cardProgramRepository;
        this.transactionControlRepository =
                transactionControlRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TxnControlsFetchResponse getTransactionControls(
            TxnControlsFetchRequest request,
            String requestId,
            String channel,
            String partnerId) {

        log.info(
                "Reading transaction controls requestId={} partnerId={} channel={}",
                requestId,
                partnerId,
                channel
        );

        TxnControlsFetchResponse response =
                new TxnControlsFetchResponse();

        response.setReferenceId(requestId);

        List<TxnControlsFetchResponse.CardControls> results =
                new ArrayList<>();

        Set<String> uniqueCardIds =
                new HashSet<>();

        String requestedCustomerId =
                request.getCustomerId();

        for (TxnControlsFetchRequest.CardRef cardRef :
                request.getCards()) {

            String cardId =
                    cardRef.getCardId();

            /*
             * De-duplicate card IDs.
             */
            if (!uniqueCardIds.add(cardId)) {
                continue;
            }

            Optional<Card> cardOptional =
                    cardRepository.findByCardId(cardId);

            /*
             * Unknown card:
             *
             * Ignore it and continue so that known cards
             * can still be returned.
             */
            if (cardOptional.isEmpty()) {

                log.info(
                        "Card not found while reading transaction controls " +
                        "requestId={} cardId={}",
                        requestId,
                        cardId
                );

                continue;
            }

            Card card =
                    cardOptional.get();

            /*
             * Ownership check.
             *
             * This follows the same ownership behavior
             * currently used by API 2:
             * when customerId is supplied, it must match
             * the card's customerId.
             */
            if (requestedCustomerId != null
                    && !requestedCustomerId.isBlank()
                    && !requestedCustomerId.equals(
                            card.getCustomerId())) {

                log.info(
                        "Customer-card ownership mismatch " +
                        "requestId={} cardId={}",
                        requestId,
                        cardId
                );

                response.setResponseCode("90");
                response.setResponseDesc(
                        "Customer-card ownership mismatch"
                );
                response.setChannels(
                        new ArrayList<>()
                );

                return response;
            }

            /*
             * Build the complete seven-channel response.
             */
            TxnControlsFetchResponse.CardControls cardControls =
                    new TxnControlsFetchResponse.CardControls();

            cardControls.setCardId(
                    card.getCardId()
            );

            List<TxnControlsFetchResponse.ControlRow> controls =
                    buildControlsForCard(card);

            cardControls.setLists(controls);

            results.add(cardControls);
        }

        response.setChannels(results);

        /*
         * No cards could be resolved.
         */
        if (results.isEmpty()) {

            response.setResponseCode("10");
            response.setResponseDesc(
                    "No transaction controls found"
            );

            return response;
        }

        /*
         * At least one card was resolved.
         *
         * This also supports partial success when
         * some requested card IDs are unknown.
         */
        response.setResponseCode("00");
        response.setResponseDesc(
                "Transaction channel controls retrieved successfully"
        );

        return response;
    }

    private List<TxnControlsFetchResponse.ControlRow>
    buildControlsForCard(Card card) {

        List<TxnControlsFetchResponse.ControlRow> controls =
                new ArrayList<>();

        for (String channelType : CHANNEL_TYPES) {

            TxnControlsFetchResponse.ControlRow row =
                    new TxnControlsFetchResponse.ControlRow();

            row.setChannelType(channelType);

            /*
            * First check whether API 5 has previously
            * persisted a control for this card/channel.
            */
            Optional<TransactionControl> savedControl =
                    transactionControlRepository
                            .findByCardIdAndChannelType(
                                    card.getCardId(),
                                    channelType
                            );

            if (savedControl.isPresent()) {

                TransactionControl control =
                        savedControl.get();

                row.setAllowed(
                        control.isAllowed()
                );

                row.setEditable(
                        control.isEditable()
                );

            } else {

                /*
                * No saved override exists.
                * Use the normal API-4 default.
                */
                applyCardStatusRules(
                        row,
                        card.getCardStatus()
                );
            }

            /*
            * Blocked/replaced/inactive cards must never expose
            * an enabled transaction channel, even if an old
            * persisted control exists.
            */
            if ("B".equals(card.getCardStatus())
                    || "R".equals(card.getCardStatus())
                    || "I".equals(card.getCardStatus())) {

                row.setAllowed(false);

                if ("DOM".equals(channelType)) {
                    row.setEditable(false);
                }
            }

            controls.add(row);
        }

        return controls;
    }

    private void applyCardStatusRules(
            TxnControlsFetchResponse.ControlRow row,
            String cardStatus) {

        /*
         * Active card:
         *
         * Default controls follow the sample specification.
         */
        if ("A".equals(cardStatus)) {

            switch (row.getChannelType()) {

                case "ATM":
                case "POS":
                case "ECOM":
                case "NFC":
                case "INT":
                    row.setAllowed(true);
                    row.setEditable(true);
                    break;

                case "MAG":
                    row.setAllowed(false);
                    row.setEditable(true);
                    break;

                case "DOM":
                    row.setAllowed(false);
                    row.setEditable(false);
                    break;

                default:
                    row.setAllowed(false);
                    row.setEditable(false);
            }

            return;
        }

        /*
         * Blocked / replaced / inactive cards:
         *
         * Card transaction channels are disabled.
         *
         * DOM remains non-editable because the sample
         * program policy marks it as locked.
         */
        if ("B".equals(cardStatus)
                || "R".equals(cardStatus)
                || "I".equals(cardStatus)) {

            row.setAllowed(false);

            if ("DOM".equals(row.getChannelType())) {
                row.setEditable(false);
            } else {
                row.setEditable(true);
            }

            return;
        }

        /*
         * Suspended or unknown status:
         *
         * Do not expose an enabled control.
         */
        row.setAllowed(false);
        row.setEditable(false);
    }
}