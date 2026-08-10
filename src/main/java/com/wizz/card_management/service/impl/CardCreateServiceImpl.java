package com.wizz.card_management.service.impl;

import com.wizz.card_management.dto.request.CreateCardRequest;
import com.wizz.card_management.dto.response.CreateCardResponse;
import com.wizz.card_management.entity.Card;
import com.wizz.card_management.entity.CardProgram;
import com.wizz.card_management.entity.IdempotencyRecord;
import com.wizz.card_management.repository.CardProgramRepository;
import com.wizz.card_management.repository.CardRepository;
import com.wizz.card_management.repository.IdempotencyRecordRepository;
import com.wizz.card_management.service.CardCreateService;
import com.wizz.card_management.util.HashUtil;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CardCreateServiceImpl implements CardCreateService {

    private final CardRepository cardRepository;
    private final CardProgramRepository cardProgramRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public CardCreateServiceImpl(
            CardRepository cardRepository,
            CardProgramRepository cardProgramRepository,
            IdempotencyRecordRepository idempotencyRecordRepository) {

        this.cardRepository = cardRepository;
        this.cardProgramRepository = cardProgramRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Override
    public CreateCardResponse createCard(
            CreateCardRequest request,
            String requestId,
            String idempotencyKey,
            String channel) {


        //  Extract request data


        String programId = request.getCard().getCardProgramId();
        String programType = request.getCard().getCardProgramType();
        String cardType = request.getCard().getCardType();


        //  Create request hash for idempotency


        String requestData =
                programType + "|" +
                cardType + "|" +
                programId;

        String requestHash = HashUtil.sha256(requestData);


        // Check existing idempotency key


        Optional<IdempotencyRecord> existingRecord =
                idempotencyRecordRepository
                        .findByIdempotencyKey(idempotencyKey);

        if (existingRecord.isPresent()) {

            IdempotencyRecord record = existingRecord.get();

            // Same key + different request = conflict
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IllegalStateException(
                        "IDEMPOTENCY_CONFLICT"
                );
            }

            // Same key + same request = replay
            return buildResponseFromRecord(record);
        }


        //  Validate card program


        CardProgram cardProgram = cardProgramRepository
                .findByProgramId(programId)
                .orElse(null);

        if (cardProgram == null) {

            CreateCardResponse response = new CreateCardResponse();

            response.setReferenceId(requestId);
            response.setResponseCode("10");
            response.setResponseDesc("Invalid card program ID");

            return response;
        }


        //  Check program active


        if (!cardProgram.isActive()) {

            CreateCardResponse response = new CreateCardResponse();

            response.setReferenceId(requestId);
            response.setResponseCode("10");
            response.setResponseDesc("Card program is inactive");

            return response;
        }


        //  Check program type


        if (!cardProgram.getProgramType().equals(programType)) {

            CreateCardResponse response = new CreateCardResponse();

            response.setReferenceId(requestId);
            response.setResponseCode("10");
            response.setResponseDesc("Card program type mismatch");

            return response;
        }


        //  Generate card ID


        String cardId = String.valueOf(
                System.currentTimeMillis()
        );


        //  Generate demo card number


        String cardNumber = "411111111111" +
                String.format(
                        "%04d",
                        (int) (Math.random() * 10000)
                );


        //  Mask card number


        String maskedCardNumber =
                cardNumber.substring(0, 4)
                        + "XXXXXXXX"
                        + cardNumber.substring(
                                cardNumber.length() - 4
                        );


        //  Generate expiry date


        String expiryDate = "07/2031";


        //  Create Card entity


        Card card = new Card();

        card.setCardId(cardId);
        card.setCardProgramType(programType);
        card.setCardType(cardType);
        card.setCardProgramId(programId);
        card.setCardNumber(cardNumber);
        card.setExpiryDate(expiryDate);
        card.setCardStatus("A");


        //  Save card


        cardRepository.save(card);


        //  Create API response


        String referenceId = requestId != null
                ? requestId
                : UUID.randomUUID().toString();

        CreateCardResponse response = new CreateCardResponse();

        response.setCardNumber(maskedCardNumber);
        response.setExpiryDate(expiryDate);
        response.setCardId(cardId);
        response.setReferenceId(referenceId);
        response.setResponseCode("00");
        response.setResponseDesc("Card created successfully");


        //  Save idempotency record


        IdempotencyRecord record = new IdempotencyRecord();

        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setCardNumber(maskedCardNumber);
        record.setExpiryDate(expiryDate);
        record.setCardId(cardId);
        record.setReferenceId(referenceId);
        record.setResponseCode("00");
        record.setResponseDesc("Card created successfully");

        idempotencyRecordRepository.save(record);


        // Return response


        return response;
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