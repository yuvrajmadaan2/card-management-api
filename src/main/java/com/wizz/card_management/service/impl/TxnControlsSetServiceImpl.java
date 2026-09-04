package com.wizz.card_management.service.impl;

import com.wizz.card_management.dto.request.TxnControlsSetRequest;
import com.wizz.card_management.dto.response.TxnControlsSetResponse;
import com.wizz.card_management.entity.Card;
import com.wizz.card_management.entity.TransactionControl;
import com.wizz.card_management.repository.CardRepository;
import com.wizz.card_management.repository.TransactionControlRepository;
import com.wizz.card_management.service.TxnControlsSetService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class TxnControlsSetServiceImpl
        implements TxnControlsSetService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TxnControlsSetServiceImpl.class
            );

    private static final Set<String> VALID_CHANNEL_TYPES =
            Set.of(
                    "ATM",
                    "POS",
                    "ECOM",
                    "NFC",
                    "MAG",
                    "DOM",
                    "INT"
            );

    private final CardRepository cardRepository;

    private final TransactionControlRepository
            transactionControlRepository;

    public TxnControlsSetServiceImpl(
            CardRepository cardRepository,
            TransactionControlRepository
                    transactionControlRepository) {

        this.cardRepository = cardRepository;
        this.transactionControlRepository =
                transactionControlRepository;
    }

    @Override
    @Transactional
    public TxnControlsSetResponse setTransactionControl(
            TxnControlsSetRequest request,
            String requestId,
            String channel,
            String partnerId) {

        log.info(
                "Setting transaction control requestId={} " +
                "partnerId={} channel={} cardId={} controlChannel={}",
                requestId,
                partnerId,
                channel,
                request.getCardId(),
                request.getChannel().getChannelType()
        );

        TxnControlsSetResponse response =
                new TxnControlsSetResponse();

        response.setReferenceId(requestId);
        response.setCardId(request.getCardId());

        String cardId =
                request.getCardId();

        String channelType =
                request.getChannel().getChannelType();

        Boolean requestedAllowed =
                request.getChannel().getAllowed();

        // Validate channel type
        if (!VALID_CHANNEL_TYPES.contains(channelType)) {

                log.warn(
                        "Invalid transaction channel type requestId={} cardId={} channelType={}",
                        requestId,
                        cardId,
                        channelType
                );

            response.setResponseCode("01");

            response.setResponseDesc(
                    "Invalid transaction channel type"
            );

            return response;
        }

        // DOM is not editable
        if ("DOM".equals(channelType)) {

                log.warn(
                        "Transaction channel not editable requestId={} cardId={} channelType={}",
                        requestId,
                        cardId,
                        channelType
                );

            response.setResponseCode("31");

            response.setResponseDesc(
                    "Transaction channel is not editable"
            );

            return response;
        }

        // Find card
        Card card =
                cardRepository.findByCardId(cardId)
                        .orElse(null);

        if (card == null) {

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

        // Ownership check
        String requestedCustomerId =
                request.getCustomerId();

        if (requestedCustomerId != null
                && !requestedCustomerId.isBlank()
                && !requestedCustomerId.equals(
                        card.getCustomerId())) {

            log.warn(
                    "Customer-card ownership mismatch " +
                    "requestId={} cardId={}",
                    requestId,
                    cardId
            );

            response.setResponseCode("90");

            response.setResponseDesc(
                    "Customer-card ownership mismatch"
            );

            return response;
        }

        // Blocked, replaced and inactive cards cannot
        // have transaction controls changed.
        if ("B".equals(card.getCardStatus())
                || "R".equals(card.getCardStatus())
                || "I".equals(card.getCardStatus())) {

                log.warn(
                        "Transaction control update declined due to card status " +
                        "requestId={} cardId={} cardStatus={}",
                        requestId,
                        cardId,
                        card.getCardStatus()
                );
            response.setResponseCode("31");

            response.setResponseDesc(
                    "Transaction controls cannot be updated for the current card status"
            );

            return response;
        }

        boolean controlExists = true;

        TransactionControl control =
                transactionControlRepository
                        .findByCardIdAndChannelType(
                                cardId,
                                channelType
                        )
                        .orElse(null);
        // If no persisted control exists, create one
        if (control == null) {

            controlExists = false;

            control = new TransactionControl();

            control.setCardId(cardId);

            control.setChannelType(channelType);

            control.setEditable(true);

            control.setAllowed(false);
        }

        // Protect non-editable controls
        if (!control.isEditable()) {

                log.warn(
                        "Transaction control update declined because control is not editable " +
                        "requestId={} cardId={} channelType={}",
                        requestId,
                        cardId,
                        channelType
                );

            response.setResponseCode("31");

            response.setResponseDesc(
                    "Transaction channel is not editable"
            );

            TxnControlsSetResponse.Channel responseChannel =
                    new TxnControlsSetResponse.Channel();

            responseChannel.setChannelType(
                    control.getChannelType()
            );

            responseChannel.setAllowed(
                    control.isAllowed()
            );

            responseChannel.setEditable(
                    control.isEditable()
            );

            response.setChannel(responseChannel);

            return response;
        }

        // No-op: requested value is already the current value
        if (controlExists && control.isAllowed() == requestedAllowed) {

            TxnControlsSetResponse.Channel responseChannel =
                    new TxnControlsSetResponse.Channel();

            responseChannel.setChannelType(
                    control.getChannelType()
            );

            responseChannel.setAllowed(
                    control.isAllowed()
            );

            responseChannel.setEditable(
                    control.isEditable()
            );

            response.setChannel(responseChannel);

            response.setResponseCode("00");

            response.setResponseDesc(
                    "Transaction control already has the requested value"
            );

            return response;
        }

        // Apply requested value
        control.setAllowed(requestedAllowed);

        transactionControlRepository.save(control);

        log.info(
                "Transaction control updated successfully " +
                "requestId={} cardId={} channelType={} allowed={}",
                requestId,
                cardId,
                channelType,
                requestedAllowed
        );

        // Build successful response
        TxnControlsSetResponse.Channel responseChannel =
                new TxnControlsSetResponse.Channel();

        responseChannel.setChannelType(
                control.getChannelType()
        );

        responseChannel.setAllowed(
                control.isAllowed()
        );

        responseChannel.setEditable(
                control.isEditable()
        );

        response.setChannel(responseChannel);

        response.setResponseCode("00");

        response.setResponseDesc(
                "Transaction control updated successfully"
        );

        return response;
    }
}