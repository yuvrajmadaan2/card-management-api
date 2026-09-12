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

        // API 2 customer/card details
        card.setNameOnCard("Chuck Yeager");
        card.setCustomerId("0012342");
        card.setIssuedDate("2026-07-07");

        cardProgram = new CardProgram();

        cardProgram.setProgramId("PRGM001");
        cardProgram.setProgramName(
                "WizzPlus Multicurrency Prepaid"
        );
    }

    @Test
    void getCardDetails_success_returnsCardDetails() {

        CardDetailsRequest request =
                createRequest(
                        "CARD001",
                        "0012342"
                );

        when(cardRepository.findByCardIdAndPartnerId("CARD001", "partner-forex-uk"))
                .thenReturn(Optional.of(card));

        when(cardProgramRepository.findByProgramIdAndPartnerId("PRGM001", "partner-forex-uk"))
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

        assertEquals(
                "Chuck Yeager",
                detail.getNameOnCard()
        );

        assertEquals(
                "0012342",
                detail.getCustomerId()
        );

        assertEquals(
                "2026-07-07",
                detail.getIssuedDate()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardIdAndPartnerId("CARD001", "partner-forex-uk");

        verify(
                cardProgramRepository,
                times(1)
        ).findByProgramIdAndPartnerId("PRGM001", "partner-forex-uk");
    }

    @Test
    void getCardDetails_cardNotFound_returnsBusinessDecline() {

        CardDetailsRequest request =
                createRequest(
                        "UNKNOWN",
                        "0012342"
                );

        when(cardRepository.findByCardIdAndPartnerId("UNKNOWN", "partner-forex-uk"))
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
        ).findByCardIdAndPartnerId("UNKNOWN", "partner-forex-uk");

        verifyNoInteractions(
                cardProgramRepository
        );
    }

    @Test
    void getCardDetails_duplicateCardIds_returnsOnlyOneCard() {

        CardDetailsRequest request =
                createRequest(
                        "CARD001",
                        "CARD001",
                        "0012342"
                );

        when(cardRepository.findByCardIdAndPartnerId("CARD001", "partner-forex-uk"))
                .thenReturn(Optional.of(card));

        when(cardProgramRepository.findByProgramIdAndPartnerId("PRGM001", "partner-forex-uk"))
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
        ).findByCardIdAndPartnerId("CARD001", "partner-forex-uk");
    }

    @Test
    void getCardDetails_partialSuccess_returnsFoundCards() {

        CardDetailsRequest request =
                createRequest(
                        "CARD001",
                        "UNKNOWN",
                        "0012342"
                );

        when(cardRepository.findByCardIdAndPartnerId("CARD001", "partner-forex-uk"))
                .thenReturn(Optional.of(card));

        when(cardRepository.findByCardIdAndPartnerId("UNKNOWN", "partner-forex-uk"))
                .thenReturn(Optional.empty());

        when(cardProgramRepository.findByProgramIdAndPartnerId("PRGM001", "partner-forex-uk"))
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
        ).findByCardIdAndPartnerId("CARD001", "partner-forex-uk");

        verify(
                cardRepository,
                times(1)
        ).findByCardIdAndPartnerId("UNKNOWN", "partner-forex-uk");
    }

    @Test
    void getCardDetails_ownershipMismatch_returns90() {

        CardDetailsRequest request =
                createRequest(
                        "CARD001",
                        "9999999"
                );

        when(cardRepository.findByCardIdAndPartnerId("CARD001", "partner-forex-uk"))
                .thenReturn(Optional.of(card));

        CardDetailsResponse response =
                service.getCardDetails(
                        request,
                        "REQ005",
                        "PARTNER",
                        "partner-forex-uk"
                );

        assertEquals(
                "REQ005",
                response.getReferenceId()
        );

        assertEquals(
                "90",
                response.getResponseCode()
        );

        assertEquals(
                "Customer–card ownership mismatch — details request declined",
                response.getResponseDesc()
        );

        assertNotNull(response.getCards());

        assertTrue(
                response.getCards().isEmpty()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardIdAndPartnerId("CARD001", "partner-forex-uk");

        verifyNoInteractions(
                cardProgramRepository
        );
    }

    @Test
    void getCardDetails_withoutCustomerId_returnsCardDetails() {

        CardDetailsRequest request =
                createRequest("CARD001");

        when(cardRepository.findByCardIdAndPartnerId("CARD001", "partner-forex-uk"))
                .thenReturn(Optional.of(card));

        when(cardProgramRepository.findByProgramIdAndPartnerId("PRGM001", "partner-forex-uk"))
                .thenReturn(Optional.of(cardProgram));

        CardDetailsResponse response =
                service.getCardDetails(
                        request,
                        "REQ006",
                        "PARTNER",
                        "partner-forex-uk"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                1,
                response.getCards().size()
        );

        assertEquals(
                "0012342",
                response.getCards()
                        .get(0)
                        .getCustomerId()
        );
    }

    @Test
    void getCardDetails_repositoryException_propagatesException() {

        CardDetailsRequest request =
                createRequest(
                        "CARD001",
                        "0012342"
                );

        when(cardRepository.findByCardIdAndPartnerId("CARD001", "partner-forex-uk"))
                .thenThrow(
                        new RuntimeException(
                                "Database unavailable"
                        )
                );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> service.getCardDetails(
                                request,
                                "REQ007",
                                "PARTNER",
                                "partner-forex-uk"
                        )
                );

        assertEquals(
                "Database unavailable",
                exception.getMessage()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardIdAndPartnerId("CARD001", "partner-forex-uk");

        verifyNoInteractions(
                cardProgramRepository
        );
    }

    private CardDetailsRequest createRequest(
            String cardId) {

        return createRequest(
                cardId,
                null
        );
    }

    private CardDetailsRequest createRequest(
            String cardId,
            String customerId) {

        CardDetailsRequest request =
                new CardDetailsRequest();

        CardDetailsRequest.CardRef ref =
                new CardDetailsRequest.CardRef();

        ref.setCardId(cardId);

        request.setCards(
                List.of(ref)
        );

        request.setCustomerId(
                customerId
        );

        return request;
    }

    private CardDetailsRequest createRequest(
            String firstCardId,
            String secondCardId,
            String customerId) {

        CardDetailsRequest request =
                new CardDetailsRequest();

        List<CardDetailsRequest.CardRef> cards =
                Arrays.stream(
                        new String[]{
                                firstCardId,
                                secondCardId
                        }
                )
                .map(cardId -> {

                    CardDetailsRequest.CardRef ref =
                            new CardDetailsRequest.CardRef();

                    ref.setCardId(cardId);

                    return ref;
                })
                .toList();

        request.setCards(cards);
        request.setCustomerId(customerId);

        return request;
    }
}