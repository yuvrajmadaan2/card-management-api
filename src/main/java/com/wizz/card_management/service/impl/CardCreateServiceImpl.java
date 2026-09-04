package com.wizz.card_management.service.impl;

import com.wizz.card_management.dto.request.CreateCardRequest;
import com.wizz.card_management.dto.response.CreateCardResponse;
import com.wizz.card_management.entity.Card;
import com.wizz.card_management.entity.CardProgram;
import com.wizz.card_management.entity.IdempotencyRecord;
import com.wizz.card_management.exception.IdempotencyConflictException;
import com.wizz.card_management.repository.CardProgramRepository;
import com.wizz.card_management.repository.CardRepository;
import com.wizz.card_management.repository.IdempotencyRecordRepository;
import com.wizz.card_management.service.CardCreateService;
import com.wizz.card_management.util.HashUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

@Service
public class CardCreateServiceImpl implements CardCreateService {

    private static final Logger log =
            LoggerFactory.getLogger(CardCreateServiceImpl.class);

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final CardRepository cardRepository;
    private final CardProgramRepository cardProgramRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public CardCreateServiceImpl(
            CardRepository cardRepository,
            CardProgramRepository cardProgramRepository,
            IdempotencyRecordRepository idempotencyRecordRepository) {

        this.cardRepository = cardRepository;
        this.cardProgramRepository = cardProgramRepository;
        this.idempotencyRecordRepository =
                idempotencyRecordRepository;
    }

    @Transactional
    @Override
    public CreateCardResponse createCard(
            CreateCardRequest request,
            String requestId,
            String idempotencyKey,
            String channel,
            String partnerId) {

        log.info(
                "Creating card requestId={} partnerId={} idempotencyKey={}",
                requestId,
                partnerId,
                idempotencyKey
        );

        // Extract request data
        String programId =
                request.getCard().getCardProgramId();

        String programType =
                request.getCard().getCardProgramType();

        String cardType =
                request.getCard().getCardType();

        // Create request hash for idempotency
        String requestData =
                programType + "|" +
                cardType + "|" +
                programId;

        String requestHash =
                HashUtil.sha256(requestData);

        // Check existing idempotency key
        Optional<IdempotencyRecord> existingRecord =
                idempotencyRecordRepository
                        .findByIdempotencyKey(idempotencyKey);

        if (existingRecord.isPresent()) {

            IdempotencyRecord record =
                    existingRecord.get();

            // Same key + different request = conflict
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException();
            }

            // Same key + same request = replay
            return buildResponseFromRecord(record);
        }

        // Validate card program
        CardProgram cardProgram =
                cardProgramRepository
                        .findByProgramId(programId)
                        .orElse(null);

        if (cardProgram == null) {

            CreateCardResponse response =
                    new CreateCardResponse();

            response.setReferenceId(requestId);
            response.setResponseCode("10");
            response.setResponseDesc(
                    "Invalid card program ID"
            );

            log.warn(
                    "Card creation declined requestId={} partnerId={} reason={}",
                    requestId,
                    partnerId,
                    response.getResponseDesc()
            );

            saveIdempotencyRecord(
                    idempotencyKey,
                    requestHash,
                    response
            );

            return response;
        }

        // Check program active
        if (!cardProgram.isActive()) {

            CreateCardResponse response =
                    new CreateCardResponse();

            response.setReferenceId(requestId);
            response.setResponseCode("10");
            response.setResponseDesc(
                    "Card program is inactive"
            );

            log.warn(
                    "Card creation declined requestId={} partnerId={} reason={}",
                    requestId,
                    partnerId,
                    response.getResponseDesc()
            );

            saveIdempotencyRecord(
                    idempotencyKey,
                    requestHash,
                    response
            );

            return response;
        }

        // Check program type
        if (!cardProgram.getProgramType().equals(programType)) {

            CreateCardResponse response =
                    new CreateCardResponse();

            response.setReferenceId(requestId);
            response.setResponseCode("10");
            response.setResponseDesc(
                    "Card program type mismatch"
            );

            log.warn(
                    "Card creation declined requestId={} partnerId={} reason={}",
                    requestId,
                    partnerId,
                    response.getResponseDesc()
            );

            saveIdempotencyRecord(
                    idempotencyKey,
                    requestHash,
                    response
            );

            return response;
        }

        // Generate card ID
        String cardId =
                UUID.randomUUID().toString();

        // Generate demo card number
        String cardNumber =
                "411111111111" +
                String.format(
                        "%04d",
                        SECURE_RANDOM.nextInt(10000)
                );

        // Mask card number
        String maskedCardNumber =
                cardNumber.substring(0, 4)
                        + "XXXXXXXX"
                        + cardNumber.substring(
                                cardNumber.length() - 4
                        );

        // Generate expiry date
        String expiryDate = "07/2031";

        // Create Card entity
        Card card = new Card();

        card.setCardId(cardId);
        card.setCardProgramType(programType);
        card.setCardType(cardType);
        card.setCardProgramId(programId);
        card.setCardNumber(maskedCardNumber);
        card.setExpiryDate(expiryDate);
        card.setCardStatus("A");

        // Save card
        cardRepository.save(card);

        log.info(
                "Card created successfully requestId={} partnerId={} cardId={}",
                requestId,
                partnerId,
                cardId
        );

        // Create API response
        String referenceId =
                requestId != null
                        ? requestId
                        : UUID.randomUUID().toString();

        CreateCardResponse response =
                new CreateCardResponse();

        response.setCardNumber(maskedCardNumber);
        response.setExpiryDate(expiryDate);
        response.setCardId(cardId);
        response.setReferenceId(referenceId);
        response.setResponseCode("00");
        response.setResponseDesc(
                "Card created successfully"
        );

        // Save idempotency record
        saveIdempotencyRecord(
                idempotencyKey,
                requestHash,
                response
        );

        return response;
    }

    private void saveIdempotencyRecord(
            String idempotencyKey,
            String requestHash,
            CreateCardResponse response) {

        IdempotencyRecord record =
                new IdempotencyRecord();

        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setCardNumber(response.getCardNumber());
        record.setExpiryDate(response.getExpiryDate());
        record.setCardId(response.getCardId());
        record.setReferenceId(response.getReferenceId());
        record.setResponseCode(response.getResponseCode());
        record.setResponseDesc(response.getResponseDesc());

        idempotencyRecordRepository.save(record);
    }

    private CreateCardResponse buildResponseFromRecord(
            IdempotencyRecord record) {

        CreateCardResponse response =
                new CreateCardResponse();

        response.setCardNumber(record.getCardNumber());
        response.setExpiryDate(record.getExpiryDate());
        response.setCardId(record.getCardId());
        response.setReferenceId(record.getReferenceId());
        response.setResponseCode(record.getResponseCode());
        response.setResponseDesc(record.getResponseDesc());

        return response;
    }
}