package com.wizz.card_management.service.impl;

import com.wizz.card_management.dto.request.TxnControlsSetRequest;
import com.wizz.card_management.dto.response.TxnControlsSetResponse;
import com.wizz.card_management.entity.Card;
import com.wizz.card_management.entity.TransactionControl;
import com.wizz.card_management.repository.CardRepository;
import com.wizz.card_management.repository.TransactionControlRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TxnControlsSetServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransactionControlRepository transactionControlRepository;

    @InjectMocks
    private TxnControlsSetServiceImpl service;

    private Card activeCard;
    private Card blockedCard;

    @BeforeEach
    void setUp() {

        activeCard = new Card();
        activeCard.setCardId("CARD001");
        activeCard.setCustomerId("CUSTOMER001");
        activeCard.setCardStatus("A");

        blockedCard = new Card();
        blockedCard.setCardId("CARD002");
        blockedCard.setCustomerId("CUSTOMER001");
        blockedCard.setCardStatus("B");
    }

    @Test
    void setTransactionControl_validRequest_createsAndSavesControl() {

        TxnControlsSetRequest request =
                createRequest(
                        "CARD001",
                        "CUSTOMER001",
                        "ATM",
                        true
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(activeCard));

        when(transactionControlRepository
                .findByCardIdAndChannelType(
                        "CARD001",
                        "ATM"
                ))
                .thenReturn(Optional.empty());

        when(transactionControlRepository.save(
                any(TransactionControl.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0));

        TxnControlsSetResponse response =
                service.setTransactionControl(
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
                "CARD001",
                response.getCardId()
        );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                "Transaction control updated successfully",
                response.getResponseDesc()
        );

        verify(transactionControlRepository)
                .save(argThat(control ->
                        "CARD001".equals(
                                control.getCardId()
                        )
                        && "ATM".equals(
                                control.getChannelType()
                        )
                        && control.isAllowed()
                        && control.isEditable()
                ));
    }

    @Test
    void setTransactionControl_existingControl_updatesAllowedValue() {

        TxnControlsSetRequest request =
                createRequest(
                        "CARD001",
                        "CUSTOMER001",
                        "POS",
                        false
                );

        TransactionControl existingControl =
                new TransactionControl();

        existingControl.setCardId("CARD001");
        existingControl.setChannelType("POS");
        existingControl.setAllowed(true);
        existingControl.setEditable(true);

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(activeCard));

        when(transactionControlRepository
                .findByCardIdAndChannelType(
                        "CARD001",
                        "POS"
                ))
                .thenReturn(
                        Optional.of(existingControl)
                );

        when(transactionControlRepository.save(
                any(TransactionControl.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0));

        TxnControlsSetResponse response =
                service.setTransactionControl(
                        request,
                        "REQ-002",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertFalse(
                existingControl.isAllowed()
        );

        verify(transactionControlRepository)
                .save(existingControl);
    }

    @Test
    void setTransactionControl_invalidChannel_returns01() {

        TxnControlsSetRequest request =
                createRequest(
                        "CARD001",
                        "CUSTOMER001",
                        "INVALID",
                        true
                );

        TxnControlsSetResponse response =
                service.setTransactionControl(
                        request,
                        "REQ-003",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "01",
                response.getResponseCode()
        );

        assertEquals(
                "Invalid transaction channel type",
                response.getResponseDesc()
        );

        verifyNoInteractions(cardRepository);
        verifyNoInteractions(
                transactionControlRepository
        );
    }

    @Test
    void setTransactionControl_domChannel_returns31() {

        TxnControlsSetRequest request =
                createRequest(
                        "CARD001",
                        "CUSTOMER001",
                        "DOM",
                        true
                );

        TxnControlsSetResponse response =
                service.setTransactionControl(
                        request,
                        "REQ-004",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "31",
                response.getResponseCode()
        );

        assertEquals(
                "Transaction channel is not editable",
                response.getResponseDesc()
        );

        verifyNoInteractions(cardRepository);
        verifyNoInteractions(
                transactionControlRepository
        );
    }

    @Test
    void setTransactionControl_unknownCard_returns10() {

        TxnControlsSetRequest request =
                createRequest(
                        "UNKNOWN",
                        "CUSTOMER001",
                        "ATM",
                        true
                );

        when(cardRepository.findByCardId("UNKNOWN"))
                .thenReturn(Optional.empty());

        TxnControlsSetResponse response =
                service.setTransactionControl(
                        request,
                        "REQ-005",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "10",
                response.getResponseCode()
        );

        assertEquals(
                "Card not found",
                response.getResponseDesc()
        );

        verifyNoInteractions(
                transactionControlRepository
        );
    }

    @Test
    void setTransactionControl_wrongCustomer_returns90() {

        TxnControlsSetRequest request =
                createRequest(
                        "CARD001",
                        "CUSTOMER999",
                        "ATM",
                        true
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(activeCard));

        TxnControlsSetResponse response =
                service.setTransactionControl(
                        request,
                        "REQ-006",
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

        verifyNoInteractions(
                transactionControlRepository
        );
    }

    @Test
    void setTransactionControl_missingCustomerId_partnerChannel_succeeds() {

        TxnControlsSetRequest request =
                createRequest(
                        "CARD001",
                        null,
                        "ATM",
                        true
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(activeCard));

        when(transactionControlRepository
                .findByCardIdAndChannelType(
                        "CARD001",
                        "ATM"
                ))
                .thenReturn(Optional.empty());

        when(transactionControlRepository.save(
                any(TransactionControl.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0));

        TxnControlsSetResponse response =
                service.setTransactionControl(
                        request,
                        "REQ-007",
                        "PARTNER",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        verify(transactionControlRepository)
                .save(any(TransactionControl.class));
    }

    @Test
    void setTransactionControl_blockedCard_returns31() {

        TxnControlsSetRequest request =
                createRequest(
                        "CARD002",
                        "CUSTOMER001",
                        "ATM",
                        true
                );

        when(cardRepository.findByCardId("CARD002"))
                .thenReturn(Optional.of(blockedCard));

        TxnControlsSetResponse response =
                service.setTransactionControl(
                        request,
                        "REQ-008",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "31",
                response.getResponseCode()
        );

        assertEquals(
                "Transaction controls cannot be updated for the current card status",
                response.getResponseDesc()
        );

        verifyNoInteractions(
                transactionControlRepository
        );
    }

    @Test
    void setTransactionControl_existingNonEditableControl_returns31() {

        TxnControlsSetRequest request =
                createRequest(
                        "CARD001",
                        "CUSTOMER001",
                        "MAG",
                        true
                );

        TransactionControl existingControl =
                new TransactionControl();

        existingControl.setCardId("CARD001");
        existingControl.setChannelType("MAG");
        existingControl.setAllowed(false);
        existingControl.setEditable(false);

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(activeCard));

        when(transactionControlRepository
                .findByCardIdAndChannelType(
                        "CARD001",
                        "MAG"
                ))
                .thenReturn(
                        Optional.of(existingControl)
                );

        TxnControlsSetResponse response =
                service.setTransactionControl(
                        request,
                        "REQ-009",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "31",
                response.getResponseCode()
        );

        assertEquals(
                "Transaction channel is not editable",
                response.getResponseDesc()
        );

        verify(
                transactionControlRepository,
                never()
        ).save(any());
    }

    @Test
    void setTransactionControl_sameValue_returnsNoOp() {

        TxnControlsSetRequest request =
                createRequest(
                        "CARD001",
                        "CUSTOMER001",
                        "ATM",
                        true
                );

        TransactionControl existingControl =
                new TransactionControl();

        existingControl.setCardId("CARD001");
        existingControl.setChannelType("ATM");
        existingControl.setAllowed(true);
        existingControl.setEditable(true);

        when(cardRepository.findByCardId("CARD001"))
                .thenReturn(Optional.of(activeCard));

        when(transactionControlRepository
                .findByCardIdAndChannelType(
                        "CARD001",
                        "ATM"
                ))
                .thenReturn(Optional.of(existingControl));

        TxnControlsSetResponse response =
                service.setTransactionControl(
                        request,
                        "REQ-NOOP",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                "Transaction control already has the requested value",
                response.getResponseDesc()
        );

        assertNotNull(response.getChannel());

        assertEquals(
                "ATM",
                response.getChannel().getChannelType()
        );

        assertTrue(
                response.getChannel().getAllowed()
        );

        assertTrue(
                response.getChannel().getEditable()
        );

        verify(
                transactionControlRepository,
                never()
        ).save(any(TransactionControl.class));
    }

    @Test
    void setTransactionControl_repositoryException_propagates() {

        TxnControlsSetRequest request =
                createRequest(
                        "CARD001",
                        "CUSTOMER001",
                        "ATM",
                        true
                );

        when(cardRepository.findByCardId("CARD001"))
                .thenThrow(
                        new RuntimeException(
                                "Database unavailable"
                        )
                );

        assertThrows(
                RuntimeException.class,
                () -> service.setTransactionControl(
                        request,
                        "REQ-010",
                        "WEB",
                        "partner-001"
                )
        );
    }

    private TxnControlsSetRequest createRequest(
            String cardId,
            String customerId,
            String channelType,
            Boolean allowed) {

        TxnControlsSetRequest request =
                new TxnControlsSetRequest();

        request.setCardId(cardId);
        request.setCustomerId(customerId);

        TxnControlsSetRequest.ControlUpdate control =
                new TxnControlsSetRequest.ControlUpdate();

        control.setChannelType(channelType);
        control.setAllowed(allowed);

        request.setChannel(control);

        return request;
    }
}