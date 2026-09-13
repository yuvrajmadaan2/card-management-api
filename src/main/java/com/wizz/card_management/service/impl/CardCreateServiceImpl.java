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

import java.util.Optional;
import com.wizz.card_management.service.CardIssuanceService;
import com.wizz.card_management.service.IssuanceResult;

import java.util.UUID;

@Service
public class CardCreateServiceImpl implements CardCreateService {

    private static final Logger log =
            LoggerFactory.getLogger(CardCreateServiceImpl.class);



    private final CardRepository cardRepository;
    private final CardProgramRepository cardProgramRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final CardIssuanceService cardIssuanceService;

    public CardCreateServiceImpl(
            CardRepository cardRepository,
            CardProgramRepository cardProgramRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            CardIssuanceService cardIssuanceService) {

        this.cardRepository = cardRepository;
        this.cardProgramRepository = cardProgramRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.cardIssuanceService = cardIssuanceService;
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

        // Atomically claim the idempotency key before any side effect
        int claimResult =
                idempotencyRecordRepository.tryClaim(
                        partnerId,
                        idempotencyKey,
                        requestHash
                );

        if (claimResult == 0) {
        IdempotencyRecord record =
                idempotencyRecordRepository
                        .findByPartnerIdAndIdempotencyKey(
                                partnerId,
                                idempotencyKey
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Idempotency record not found after claim conflict"
                                )
                        );

        // Same key + different request = conflict
        if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException();
        }

        // Completed request = safe replay
        if ("COMPLETED".equals(record.getStatus())) {
                return buildResponseFromRecord(record);
        }

        // A persisted IN_PROGRESS record should not normally occur
        // because the claim and card creation are in the same transaction.
        throw new IllegalStateException(
                "Request with this idempotency key is already in progress"
        );
        }

        // Validate card program
        CardProgram cardProgram =
                cardProgramRepository
                        .findByProgramIdAndPartnerId(programId, partnerId)
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
                    response,
                    partnerId
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
                    response,
                    partnerId
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
                    response,
                    partnerId
            );

            return response;
        }

        // Generate card ID
        String cardId = UUID.randomUUID().toString();

        // Issue card through issuance service
        IssuanceResult issuanceResult =
                cardIssuanceService.issueCard(request);

        String maskedCardNumber =
                issuanceResult.maskedCardNumber();

        String expiryDate =
                issuanceResult.expiryDate();

        String processorReference =
                issuanceResult.processorReference();

        // Create Card entity
        Card card = new Card();

        card.setCardId(cardId);
        card.setCardProgramType(programType);
        card.setCardType(cardType);
        card.setPartnerId(partnerId);
        card.setCardProgramId(programId);
        card.setCardNumber(maskedCardNumber);
        card.setExpiryDate(expiryDate);
        card.setProcessorReference(processorReference);
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
                response,
                partnerId
        );

        return response;
    }

        private void saveIdempotencyRecord(
                String idempotencyKey,
                String requestHash,
                CreateCardResponse response,
                String partnerId) {

        IdempotencyRecord record =
                idempotencyRecordRepository
                        .findByPartnerIdAndIdempotencyKey(
                                partnerId,
                                idempotencyKey
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Idempotency record not found"
                                )
                        );

        record.setRequestHash(requestHash);
        record.setCardNumber(response.getCardNumber());
        record.setExpiryDate(response.getExpiryDate());
        record.setCardId(response.getCardId());
        record.setReferenceId(response.getReferenceId());
        record.setResponseCode(response.getResponseCode());
        record.setResponseDesc(response.getResponseDesc());
        record.setStatus("COMPLETED");

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