package com.wizz.card_management.service.impl;

import com.wizz.card_management.dto.request.TxnControlsFetchRequest;
import com.wizz.card_management.dto.response.TxnControlsFetchResponse;
import com.wizz.card_management.entity.Card;
import com.wizz.card_management.entity.TransactionControl;
import com.wizz.card_management.repository.CardProgramRepository;
import com.wizz.card_management.repository.CardRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.wizz.card_management.repository.TransactionControlRepository;

@ExtendWith(MockitoExtension.class)
class CardControlsReadServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardProgramRepository cardProgramRepository;

    @Mock
    private TransactionControlRepository transactionControlRepository;

    @InjectMocks
    private CardControlsReadServiceImpl service;

    private Card activeCard;
    private Card blockedCard;
    private Card replacedCard;

    @BeforeEach
    void setUp() {

        activeCard = new Card();
        activeCard.setCardId("CARD001");
        activeCard.setCardProgramId("PROGRAM001");
        activeCard.setCardStatus("A");
        activeCard.setCustomerId("CUSTOMER001");

        blockedCard = new Card();
        blockedCard.setCardId("CARD002");
        blockedCard.setCardProgramId("PROGRAM001");
        blockedCard.setCardStatus("B");
        blockedCard.setCustomerId("CUSTOMER001");

        replacedCard = new Card();
        replacedCard.setCardId("CARD003");
        replacedCard.setCardProgramId("PROGRAM001");
        replacedCard.setCardStatus("R");
        replacedCard.setCustomerId("CUSTOMER001");

        lenient().when(transactionControlRepository
                .findByCardIdAndChannelType(
                        anyString(),
                        anyString()
                ))
                .thenReturn(Optional.empty());

    }

    @Test
    void getTransactionControls_activeCard_returnsFullSevenChannelMatrix() {

        TxnControlsFetchRequest request =
                createRequest(
                        List.of("CARD001"),
                        "CUSTOMER001"
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(activeCard));

        TxnControlsFetchResponse response =
                service.getTransactionControls(
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
                "Transaction channel controls retrieved successfully",
                response.getResponseDesc()
        );

        assertNotNull(response.getChannels());
        assertEquals(1, response.getChannels().size());

        TxnControlsFetchResponse.CardControls cardControls =
                response.getChannels().get(0);

        assertEquals(
                "CARD001",
                cardControls.getCardId()
        );

        assertNotNull(cardControls.getLists());

        assertEquals(
                7,
                cardControls.getLists().size()
        );

        assertChannel(
                cardControls,
                "ATM",
                true,
                true
        );

        assertChannel(
                cardControls,
                "POS",
                true,
                true
        );

        assertChannel(
                cardControls,
                "ECOM",
                true,
                true
        );

        assertChannel(
                cardControls,
                "NFC",
                true,
                true
        );

        assertChannel(
                cardControls,
                "MAG",
                false,
                true
        );

        assertChannel(
                cardControls,
                "DOM",
                false,
                false
        );

        assertChannel(
                cardControls,
                "INT",
                true,
                true
        );
    }

    @Test
    void getTransactionControls_duplicateCardIds_returnsOnlyOneCard() {

        TxnControlsFetchRequest request =
                createRequest(
                        List.of(
                                "CARD001",
                                "CARD001"
                        ),
                        "CUSTOMER001"
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(activeCard));

        TxnControlsFetchResponse response =
                service.getTransactionControls(
                        request,
                        "REQ-002",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                1,
                response.getChannels().size()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardId("CARD001");
    }

    @Test
    void getTransactionControls_unknownCardWithKnownCard_returnsPartialSuccess() {

        TxnControlsFetchRequest request =
                createRequest(
                        List.of(
                                "UNKNOWN",
                                "CARD001"
                        ),
                        "CUSTOMER001"
                );

        when(cardRepository.findByCardId("UNKNOWN"))
                .thenReturn(Optional.empty());

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(activeCard));

        TxnControlsFetchResponse response =
                service.getTransactionControls(
                        request,
                        "REQ-003",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                "Transaction channel controls retrieved successfully",
                response.getResponseDesc()
        );

        assertEquals(
                1,
                response.getChannels().size()
        );

        assertEquals(
                "CARD001",
                response.getChannels().get(0).getCardId()
        );
    }

    @Test
    void getTransactionControls_onlyUnknownCard_returns10() {

        TxnControlsFetchRequest request =
                createRequest(
                        List.of("UNKNOWN"),
                        "CUSTOMER001"
                );

        when(cardRepository.findByCardId("UNKNOWN"))
                .thenReturn(Optional.empty());

        TxnControlsFetchResponse response =
                service.getTransactionControls(
                        request,
                        "REQ-004",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "10",
                response.getResponseCode()
        );

        assertEquals(
                "No transaction controls found",
                response.getResponseDesc()
        );

        assertNotNull(response.getChannels());

        assertTrue(
                response.getChannels().isEmpty()
        );
    }

    @Test
    void getTransactionControls_wrongCustomer_returns90() {

        TxnControlsFetchRequest request =
                createRequest(
                        List.of("CARD001"),
                        "CUSTOMER999"
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(activeCard));

        TxnControlsFetchResponse response =
                service.getTransactionControls(
                        request,
                        "REQ-005",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "90",
                response.getResponseCode()
        );

        assertEquals(
                "Customer-card ownership mismatch",
                response.getResponseDesc()
        );

        assertNotNull(response.getChannels());

        assertTrue(
                response.getChannels().isEmpty()
        );

        verify(
                cardRepository,
                times(1)
        ).findByCardId("CARD001");
    }

    @Test
    void getTransactionControls_missingCustomerId_partnerChannel_returnsControls() {

        TxnControlsFetchRequest request =
                createRequest(
                        List.of("CARD001"),
                        null
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(activeCard));

        TxnControlsFetchResponse response =
                service.getTransactionControls(
                        request,
                        "REQ-006",
                        "PARTNER",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                1,
                response.getChannels().size()
        );

        assertEquals(
                7,
                response.getChannels()
                        .get(0)
                        .getLists()
                        .size()
        );
    }

    @Test
    void getTransactionControls_blockedCard_disablesChannels() {

        TxnControlsFetchRequest request =
                createRequest(
                        List.of("CARD002"),
                        "CUSTOMER001"
                );

        when(cardRepository.findByCardId("CARD002"))
                .thenReturn(Optional.of(blockedCard));

        TxnControlsFetchResponse response =
                service.getTransactionControls(
                        request,
                        "REQ-007",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        TxnControlsFetchResponse.CardControls controls =
                response.getChannels().get(0);

        assertEquals(
                7,
                controls.getLists().size()
        );

        for (
                TxnControlsFetchResponse.ControlRow row :
                controls.getLists()
        ) {

            assertFalse(
                    row.getAllowed(),
                    "Blocked card channel should not be allowed"
            );

            if ("DOM".equals(row.getChannelType())) {

                assertFalse(
                        row.getEditable(),
                        "DOM should remain non-editable"
                );
            }
        }
    }

    @Test
    void getTransactionControls_replacedCard_disablesChannels() {

        TxnControlsFetchRequest request =
                createRequest(
                        List.of("CARD003"),
                        "CUSTOMER001"
                );

        when(cardRepository.findByCardId("CARD003"))
                .thenReturn(Optional.of(replacedCard));

        TxnControlsFetchResponse response =
                service.getTransactionControls(
                        request,
                        "REQ-008",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        TxnControlsFetchResponse.CardControls controls =
                response.getChannels().get(0);

        assertEquals(
                7,
                controls.getLists().size()
        );

        for (
                TxnControlsFetchResponse.ControlRow row :
                controls.getLists()
        ) {

            assertFalse(
                    row.getAllowed()
            );
        }
    }

    @Test
    void getTransactionControls_nullCustomerId_customerChannel_currentImplementationReturnsControls() {

        TxnControlsFetchRequest request =
                createRequest(
                        List.of("CARD001"),
                        null
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(activeCard));

        TxnControlsFetchResponse response =
                service.getTransactionControls(
                        request,
                        "REQ-009",
                        "WEB",
                        "partner-001"
                );

        /*
         * The current API-2-compatible implementation treats
         * customerId as conditional. The exact customer-facing
         * channel requirement should follow the mentor/spec
         * clarification rather than being invented here.
         */
        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                1,
                response.getChannels().size()
        );
    }

    @Test
    void getTransactionControls_repositoryException_propagates() {

        TxnControlsFetchRequest request =
                createRequest(
                        List.of("CARD001"),
                        "CUSTOMER001"
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenThrow(
                        new RuntimeException(
                                "Database unavailable"
                        )
                );

        assertThrows(
                RuntimeException.class,
                () ->
                        service.getTransactionControls(
                                request,
                                "REQ-010",
                                "WEB",
                                "partner-001"
                        )
        );
    }

    @Test
    void getTransactionControls_savedControl_returnsPersistedValue() {

        Card card = new Card();
        card.setCardId("CARD001");
        card.setCustomerId("CUSTOMER001");
        card.setCardStatus("A");

        TxnControlsFetchRequest request =
                new TxnControlsFetchRequest();

        request.setCustomerId("CUSTOMER001");

        TxnControlsFetchRequest.CardRef cardRef =
                new TxnControlsFetchRequest.CardRef();

        cardRef.setCardId("CARD001");

        request.setCards(
                List.of(cardRef)
        );

        TransactionControl savedControl =
                new TransactionControl();

        savedControl.setCardId("CARD001");
        savedControl.setChannelType("ATM");
        savedControl.setAllowed(false);
        savedControl.setEditable(true);

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(card));

        when(transactionControlRepository
                .findByCardIdAndChannelType(
                        "CARD001",
                        "ATM"
                ))
                .thenReturn(Optional.of(savedControl));

        when(transactionControlRepository
                .findByCardIdAndChannelType(
                        eq("CARD001"),
                        argThat(channel ->
                                !channel.equals("ATM")
                        )
                ))
                .thenReturn(Optional.empty());

        TxnControlsFetchResponse response =
                service.getTransactionControls(
                        request,
                        "REQ-011",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                1,
                response.getChannels().size()
        );

        TxnControlsFetchResponse.CardControls cardControls =
                response.getChannels().get(0);

        TxnControlsFetchResponse.ControlRow atmControl =
                cardControls.getLists()
                        .stream()
                        .filter(control ->
                                "ATM".equals(
                                        control.getChannelType()
                                )
                        )
                        .findFirst()
                        .orElseThrow();

        assertFalse(
                atmControl.getAllowed()
        );

        assertTrue(
                atmControl.getEditable()
        );
    }

    private TxnControlsFetchRequest createRequest(
            List<String> cardIds,
            String customerId) {

        TxnControlsFetchRequest request =
                new TxnControlsFetchRequest();

        request.setCustomerId(customerId);

        List<TxnControlsFetchRequest.CardRef> refs =
                cardIds.stream()
                        .map(this::createCardRef)
                        .toList();

        request.setCards(refs);

        return request;
    }

    private TxnControlsFetchRequest.CardRef createCardRef(
            String cardId) {

        TxnControlsFetchRequest.CardRef ref =
                new TxnControlsFetchRequest.CardRef();

        ref.setCardId(cardId);

        return ref;
    }

    private void assertChannel(
            TxnControlsFetchResponse.CardControls cardControls,
            String channelType,
            boolean expectedAllowed,
            boolean expectedEditable) {

        TxnControlsFetchResponse.ControlRow row =
                cardControls.getLists()
                        .stream()
                        .filter(control ->
                                channelType.equals(
                                        control.getChannelType()
                                )
                        )
                        .findFirst()
                        .orElseThrow(
                                () -> new AssertionError(
                                        "Channel not found: "
                                                + channelType
                                )
                        );

        assertEquals(
                expectedAllowed,
                row.getAllowed()
        );

        assertEquals(
                expectedEditable,
                row.getEditable()
        );
    }
}