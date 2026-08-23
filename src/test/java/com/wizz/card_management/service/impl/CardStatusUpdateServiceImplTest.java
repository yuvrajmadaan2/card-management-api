package com.wizz.card_management.service.impl;

import com.wizz.card_management.dto.request.SetCardStatusRequest;
import com.wizz.card_management.dto.response.SetCardStatusResponse;
import com.wizz.card_management.entity.Card;
import com.wizz.card_management.entity.CardProgram;
import com.wizz.card_management.repository.CardProgramRepository;
import com.wizz.card_management.repository.CardRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardStatusUpdateServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardProgramRepository cardProgramRepository;

    @InjectMocks
    private CardStatusUpdateServiceImpl service;

    private Card card;
    private CardProgram cardProgram;

    @BeforeEach
    void setUp() {

        card = new Card();

        card.setCardId("CARD001");
        card.setCardProgramType("P");
        card.setCardType("V");
        card.setCardProgramId("PRGM001");
        card.setCardNumber("4111XXXXXXXX1234");
        card.setExpiryDate("07/2031");
        card.setCardStatus("A");
        card.setCustomerId("0012342");
        card.setNameOnCard("Chuck Yeager");
        card.setIssuedDate("2026-07-07");

        cardProgram = new CardProgram();

        cardProgram.setProgramId("PRGM001");
        cardProgram.setProgramName(
                "WizzPlus Multicurrency Prepaid"
        );
    }

    @Test
    void updateCardStatus_success_returnsUpdatedCardDetails() {

        SetCardStatusRequest request =
                createRequest(
                        "CARD001",
                        "S",
                        "CUSTREQ",
                        "Temporary travel freeze"
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(card));

        when(cardProgramRepository.findByProgramId("PRGM001"))
                .thenReturn(Optional.of(cardProgram));

        SetCardStatusResponse response =
                service.updateCardStatus(
                        request,
                        "REQ-001",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "REQ-001",
                response.getReferenceId()
        );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                "Card status updated to TEMP SUSPENDED",
                response.getResponseDesc()
        );

        assertEquals(
                "4111XXXXXXXX1234",
                response.getCardNumber()
        );

        assertEquals(
                "WizzPlus Multicurrency Prepaid",
                response.getCardProgramName()
        );

        assertEquals(
                "0012342",
                response.getCustomerId()
        );

        assertEquals(
                "S",
                card.getCardStatus()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardId("CARD001");

        verify(
                cardRepository,
                times(1)
        ).save(card);

        verify(
                cardProgramRepository,
                times(1)
        ).findByProgramId("PRGM001");
    }

    @Test
    void updateCardStatus_missingReasonCode_returns02() {

        SetCardStatusRequest request =
                createRequest(
                        "CARD001",
                        "B",
                        null,
                        "Block card"
                );

        SetCardStatusResponse response =
                service.updateCardStatus(
                        request,
                        "REQ-002",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "REQ-002",
                response.getReferenceId()
        );

        assertEquals(
                "02",
                response.getResponseCode()
        );

        assertEquals(
                "Reason code is mandatory for the requested card status",
                response.getResponseDesc()
        );

        verifyNoInteractions(
                cardRepository
        );

        verifyNoInteractions(
                cardProgramRepository
        );
    }

    @Test
    void updateCardStatus_invalidStatusCode_returns01() {

        SetCardStatusRequest request =
                createRequest(
                        "CARD001",
                        "X",
                        "TEST",
                        "Invalid status"
                );

        SetCardStatusResponse response =
                service.updateCardStatus(
                        request,
                        "REQ-003",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "REQ-003",
                response.getReferenceId()
        );

        assertEquals(
                "01",
                response.getResponseCode()
        );

        assertEquals(
                "Invalid card status code",
                response.getResponseDesc()
        );

        verifyNoInteractions(
                cardRepository
        );

        verifyNoInteractions(
                cardProgramRepository
        );
    }

    @Test
    void updateCardStatus_cardNotFound_returns10() {

        SetCardStatusRequest request =
                createRequest(
                        "UNKNOWN",
                        "S",
                        "CUSTREQ",
                        "Temporary travel freeze"
                );

        when(cardRepository.findByCardId("UNKNOWN"))
                .thenReturn(Optional.empty());

        SetCardStatusResponse response =
                service.updateCardStatus(
                        request,
                        "REQ-004",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "REQ-004",
                response.getReferenceId()
        );

        assertEquals(
                "10",
                response.getResponseCode()
        );

        assertEquals(
                "Card not found",
                response.getResponseDesc()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardId("UNKNOWN");

        verify(
                cardRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                cardProgramRepository
        );
    }

    @Test
    void updateCardStatus_sameStatus_returns00WithoutSaving() {

        card.setCardStatus("A");

        SetCardStatusRequest request =
                createRequest(
                        "CARD001",
                        "A",
                        null,
                        null
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(card));

        SetCardStatusResponse response =
                service.updateCardStatus(
                        request,
                        "REQ-005",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                "Card status is already ACTIVE",
                response.getResponseDesc()
        );

        assertEquals(
                "4111XXXXXXXX1234",
                response.getCardNumber()
        );

        assertEquals(
                "0012342",
                response.getCustomerId()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardId("CARD001");

        verify(
                cardRepository,
                never()
        ).save(any());

        verify(
                cardProgramRepository,
                times(1)
        ).findByProgramId("PRGM001");
    }

    @Test
    void updateCardStatus_replacedCard_returns31() {

        card.setCardStatus("R");

        SetCardStatusRequest request =
                createRequest(
                        "CARD001",
                        "S",
                        "CUSTREQ",
                        "Temporary travel freeze"
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(card));

        SetCardStatusResponse response =
                service.updateCardStatus(
                        request,
                        "REQ-006",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "31",
                response.getResponseCode()
        );

        assertEquals(
                "Invalid card status transition",
                response.getResponseDesc()
        );

        assertEquals(
                "R",
                card.getCardStatus()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardId("CARD001");

        verify(
                cardRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                cardProgramRepository
        );
    }

    @Test
    void updateCardStatus_reasonCodeRequiredForSuspendedStatus() {

        SetCardStatusRequest request =
                createRequest(
                        "CARD001",
                        "S",
                        "   ",
                        "Temporary travel freeze"
                );

        SetCardStatusResponse response =
                service.updateCardStatus(
                        request,
                        "REQ-007",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "02",
                response.getResponseCode()
        );

        assertEquals(
                "Reason code is mandatory for the requested card status",
                response.getResponseDesc()
        );

        verifyNoInteractions(
                cardRepository
        );

        verifyNoInteractions(
                cardProgramRepository
        );
    }

    @Test
    void updateCardStatus_reasonCodeRequiredForReplacedStatus() {

        SetCardStatusRequest request =
                createRequest(
                        "CARD001",
                        "R",
                        null,
                        null
                );

        SetCardStatusResponse response =
                service.updateCardStatus(
                        request,
                        "REQ-008",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "02",
                response.getResponseCode()
        );

        verifyNoInteractions(
                cardRepository
        );

        verifyNoInteractions(
                cardProgramRepository
        );
    }

    private SetCardStatusRequest createRequest(
            String cardId,
            String statusCode,
            String reasonCode,
            String remarks) {

        SetCardStatusRequest request =
                new SetCardStatusRequest();

        SetCardStatusRequest.CardStatusPayload card =
                new SetCardStatusRequest.CardStatusPayload();

        card.setCardId(cardId);
        card.setStatusCode(statusCode);
        card.setReasonCode(reasonCode);
        card.setRemarks(remarks);

        request.setCard(card);

        return request;
    }
}