package com.wizz.card_management.service.impl;

import com.wizz.card_management.dto.request.CardDetailsRequest;
import com.wizz.card_management.dto.response.CardDetailsResponse;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardDetailsReadServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardProgramRepository cardProgramRepository;

    @InjectMocks
    private CardDetailsReadServiceImpl service;

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

        cardProgram = new CardProgram();

        cardProgram.setProgramId("PRGM001");
        cardProgram.setProgramName(
                "WizzPlus Multicurrency Prepaid"
        );
    }

    @Test
    void getCardDetails_success_returnsCardDetails() {

        CardDetailsRequest request =
                createRequest("CARD001");

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(card));

        when(cardProgramRepository.findByProgramId("PRGM001"))
                .thenReturn(Optional.of(cardProgram));

        CardDetailsResponse response =
                service.getCardDetails(
                        request,
                        "REQ001",
                        "PARTNER",
                        "partner-forex-uk"
                );

        assertEquals(
                "REQ001",
                response.getReferenceId()
        );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                "Card details retrieved successfully",
                response.getResponseDesc()
        );

        assertNotNull(response.getCards());

        assertEquals(
                1,
                response.getCards().size()
        );

        CardDetailsResponse.CardDetail detail =
                response.getCards().get(0);

        assertEquals(
                "CARD001",
                detail.getCardId()
        );

        assertEquals(
                "P",
                detail.getCardProgramType()
        );

        assertEquals(
                "V",
                detail.getCardType()
        );

        assertEquals(
                "PRGM001",
                detail.getCardProgramId()
        );

        assertEquals(
                "WizzPlus Multicurrency Prepaid",
                detail.getCardProgramName()
        );

        assertEquals(
                "4111XXXXXXXX1234",
                detail.getCardNumber()
        );

        assertEquals(
                "07/2031",
                detail.getExpiryDate()
        );

        assertEquals(
                "A",
                detail.getCardStatus()
        );

        assertEquals(
                "ACTIVE",
                detail.getCardStatusDesc()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardId("CARD001");

        verify(
                cardProgramRepository,
                times(1)
        ).findByProgramId("PRGM001");
    }

    @Test
    void getCardDetails_cardNotFound_returnsBusinessDecline() {

        CardDetailsRequest request =
                createRequest("UNKNOWN");

        when(cardRepository.findByCardId("UNKNOWN"))
                .thenReturn(Optional.empty());

        CardDetailsResponse response =
                service.getCardDetails(
                        request,
                        "REQ002",
                        "PARTNER",
                        "partner-forex-uk"
                );

        assertEquals(
                "REQ002",
                response.getReferenceId()
        );

        assertEquals(
                "10",
                response.getResponseCode()
        );

        assertEquals(
                "No card details found",
                response.getResponseDesc()
        );

        assertNotNull(response.getCards());

        assertTrue(
                response.getCards().isEmpty()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardId("UNKNOWN");

        verifyNoInteractions(
                cardProgramRepository
        );
    }

    @Test
    void getCardDetails_duplicateCardIds_returnsOnlyOneCard() {

        CardDetailsRequest request =
                createRequest(
                        "CARD001",
                        "CARD001"
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(card));

        when(cardProgramRepository.findByProgramId("PRGM001"))
                .thenReturn(Optional.of(cardProgram));

        CardDetailsResponse response =
                service.getCardDetails(
                        request,
                        "REQ003",
                        "PARTNER",
                        "partner-forex-uk"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertNotNull(response.getCards());

        assertEquals(
                1,
                response.getCards().size()
        );

        assertEquals(
                "CARD001",
                response.getCards().get(0).getCardId()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardId("CARD001");
    }

    @Test
    void getCardDetails_partialSuccess_returnsFoundCards() {

        CardDetailsRequest request =
                createRequest(
                        "CARD001",
                        "UNKNOWN"
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(card));

        when(cardRepository.findByCardId("UNKNOWN"))
                .thenReturn(Optional.empty());

        when(cardProgramRepository.findByProgramId("PRGM001"))
                .thenReturn(Optional.of(cardProgram));

        CardDetailsResponse response =
                service.getCardDetails(
                        request,
                        "REQ004",
                        "PARTNER",
                        "partner-forex-uk"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertNotNull(response.getCards());

        assertEquals(
                1,
                response.getCards().size()
        );

        assertEquals(
                "CARD001",
                response.getCards().get(0).getCardId()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardId("CARD001");

        verify(
                cardRepository,
                times(1)
        ).findByCardId("UNKNOWN");
    }

    private CardDetailsRequest createRequest(
            String... cardIds) {

        CardDetailsRequest request =
                new CardDetailsRequest();

        List<CardDetailsRequest.CardRef> cards =
                Arrays.stream(cardIds)
                        .map(cardId -> {

                            CardDetailsRequest.CardRef ref =
                                    new CardDetailsRequest.CardRef();

                            ref.setCardId(cardId);

                            return ref;
                        })
                        .toList();

        request.setCards(cards);

        return request;
    }
}