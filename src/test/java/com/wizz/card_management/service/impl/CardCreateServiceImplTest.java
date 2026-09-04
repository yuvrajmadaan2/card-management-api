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
import com.wizz.card_management.util.HashUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardCreateServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardProgramRepository cardProgramRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @InjectMocks
    private CardCreateServiceImpl cardCreateService;

    private CreateCardRequest request;

    @BeforeEach
    void setUp() {

        request = new CreateCardRequest();

        CreateCardRequest.CardPayload card =
                new CreateCardRequest.CardPayload();

        card.setCardProgramId("PROGRAM-001");
        card.setCardProgramType("D");
        card.setCardType("V");

        request.setCard(card);
    }

    @Test
    void validRequest_shouldCreateCardSuccessfully() {

        CardProgram program = createProgram(
                "PROGRAM-001",
                "D",
                true
        );

        when(idempotencyRecordRepository
                .findByIdempotencyKey("KEY-001"))
                .thenReturn(Optional.empty());

        when(cardProgramRepository
                .findByProgramId("PROGRAM-001"))
                .thenReturn(Optional.of(program));

        when(cardRepository.save(any(Card.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(idempotencyRecordRepository.save(
                any(IdempotencyRecord.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        CreateCardResponse response =
                cardCreateService.createCard(
                        request,
                        "REQ-001",
                        "KEY-001",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                "Card created successfully",
                response.getResponseDesc()
        );

        assertEquals(
                "REQ-001",
                response.getReferenceId()
        );

        assertNotNull(response.getCardId());
        assertNotNull(response.getCardNumber());

        assertEquals(
                "07/2031",
                response.getExpiryDate()
        );

        verify(cardRepository)
                .save(any(Card.class));

        verify(idempotencyRecordRepository)
                .save(any(IdempotencyRecord.class));
    }

    @Test
    void invalidProgram_shouldReturnDecline() {

        when(idempotencyRecordRepository
                .findByIdempotencyKey("KEY-002"))
                .thenReturn(Optional.empty());

        when(cardProgramRepository
                .findByProgramId("PROGRAM-001"))
                .thenReturn(Optional.empty());

        CreateCardResponse response =
                cardCreateService.createCard(
                        request,
                        "REQ-002",
                        "KEY-002",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "10",
                response.getResponseCode()
        );

        assertEquals(
                "Invalid card program ID",
                response.getResponseDesc()
        );

        assertEquals(
                "REQ-002",
                response.getReferenceId()
        );

        verify(cardRepository, never())
                .save(any(Card.class));

        verify(idempotencyRecordRepository)
                .save(argThat(record ->
                        "KEY-002".equals(
                                record.getIdempotencyKey()
                        )
                        && "10".equals(
                                record.getResponseCode()
                        )
                        && "Invalid card program ID".equals(
                                record.getResponseDesc()
                        )
                ));
    }

    @Test
    void inactiveProgram_shouldReturnDecline() {

        CardProgram program = createProgram(
                "PROGRAM-001",
                "D",
                false
        );

        when(idempotencyRecordRepository
                .findByIdempotencyKey("KEY-003"))
                .thenReturn(Optional.empty());

        when(cardProgramRepository
                .findByProgramId("PROGRAM-001"))
                .thenReturn(Optional.of(program));

        CreateCardResponse response =
                cardCreateService.createCard(
                        request,
                        "REQ-003",
                        "KEY-003",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "10",
                response.getResponseCode()
        );

        assertEquals(
                "Card program is inactive",
                response.getResponseDesc()
        );

        assertEquals(
                "REQ-003",
                response.getReferenceId()
        );

        verify(cardRepository, never())
                .save(any(Card.class));

        verify(idempotencyRecordRepository)
                .save(argThat(record ->
                        "KEY-003".equals(
                                record.getIdempotencyKey()
                        )
                        && "10".equals(
                                record.getResponseCode()
                        )
                        && "Card program is inactive".equals(
                                record.getResponseDesc()
                        )
                ));
    }

    @Test
    void programTypeMismatch_shouldReturnDecline() {

        CardProgram program = createProgram(
                "PROGRAM-001",
                "P",
                true
        );

        when(idempotencyRecordRepository
                .findByIdempotencyKey("KEY-004"))
                .thenReturn(Optional.empty());

        when(cardProgramRepository
                .findByProgramId("PROGRAM-001"))
                .thenReturn(Optional.of(program));

        CreateCardResponse response =
                cardCreateService.createCard(
                        request,
                        "REQ-004",
                        "KEY-004",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "10",
                response.getResponseCode()
        );

        assertEquals(
                "Card program type mismatch",
                response.getResponseDesc()
        );

        assertEquals(
                "REQ-004",
                response.getReferenceId()
        );

        verify(cardRepository, never())
                .save(any(Card.class));

        verify(idempotencyRecordRepository)
                .save(argThat(record ->
                        "KEY-004".equals(
                                record.getIdempotencyKey()
                        )
                        && "10".equals(
                                record.getResponseCode()
                        )
                        && "Card program type mismatch".equals(
                                record.getResponseDesc()
                        )
                ));
    }

    @Test
    void sameIdempotencyKeyAndSameRequest_shouldReplayOriginalResponse() {

        String requestData =
                "D|V|PROGRAM-001";

        IdempotencyRecord record =
                createIdempotencyRecord(
                        "KEY-005",
                        HashUtil.sha256(requestData)
                );

        when(idempotencyRecordRepository
                .findByIdempotencyKey("KEY-005"))
                .thenReturn(Optional.of(record));

        CreateCardResponse response =
                cardCreateService.createCard(
                        request,
                        "REQ-005",
                        "KEY-005",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                "Card created successfully",
                response.getResponseDesc()
        );

        assertEquals(
                "CARD-001",
                response.getCardId()
        );

        assertEquals(
                "REQ-005",
                response.getReferenceId()
        );

        verify(cardRepository, never())
                .save(any(Card.class));

        verify(cardProgramRepository, never())
                .findByProgramId(anyString());
    }

        @Test
        void sameIdempotencyKeyWithDifferentRequestId_shouldReplayOriginalReferenceId() {

        String requestData =
                "D|V|PROGRAM-001";

        IdempotencyRecord record =
                createIdempotencyRecord(
                        "KEY-007",
                        HashUtil.sha256(requestData)
                );

        when(idempotencyRecordRepository
                .findByIdempotencyKey("KEY-007"))
                .thenReturn(Optional.of(record));

        CreateCardResponse response =
                cardCreateService.createCard(
                        request,
                        "REQ-999",
                        "KEY-007",
                        "WEB",
                        "partner-001"
                );

        assertEquals(
                "00",
                response.getResponseCode()
        );

        assertEquals(
                "REQ-005",
                response.getReferenceId()
        );

        assertEquals(
                "CARD-001",
                response.getCardId()
        );

        verify(cardRepository, never())
                .save(any(Card.class));

        verify(cardProgramRepository, never())
                .findByProgramId(anyString());
        }

    @Test
    void sameIdempotencyKeyAndDifferentRequest_shouldThrowConflict() {

        String existingRequestData =
                "D|P|PROGRAM-001";

        IdempotencyRecord record =
                createIdempotencyRecord(
                        "KEY-006",
                        HashUtil.sha256(existingRequestData)
                );

        when(idempotencyRecordRepository
                .findByIdempotencyKey("KEY-006"))
                .thenReturn(Optional.of(record));

        assertThrows(
                IdempotencyConflictException.class,
                () -> cardCreateService.createCard(
                        request,
                        "REQ-006",
                        "KEY-006",
                        "WEB",
                        "partner-001"
                )
        );

        verify(cardRepository, never())
                .save(any(Card.class));

        verify(cardProgramRepository, never())
                .findByProgramId(anyString());
    }

    private CardProgram createProgram(
            String programId,
            String programType,
            boolean active) {

        CardProgram program =
                new CardProgram();

        program.setProgramId(programId);
        program.setProgramName("Test Program");
        program.setProgramType(programType);
        program.setActive(active);

        return program;
    }

    private IdempotencyRecord createIdempotencyRecord(
            String key,
            String requestHash) {

        IdempotencyRecord record =
                new IdempotencyRecord();

        record.setIdempotencyKey(key);
        record.setRequestHash(requestHash);
        record.setCardNumber("4111XXXXXXXX1111");
        record.setExpiryDate("07/2031");
        record.setCardId("CARD-001");
        record.setReferenceId("REQ-005");
        record.setResponseCode("00");
        record.setResponseDesc(
                "Card created successfully"
        );

        return record;
    }
}