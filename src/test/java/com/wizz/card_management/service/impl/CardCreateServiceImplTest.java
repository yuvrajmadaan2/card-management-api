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
import com.wizz.card_management.service.CardIssuanceService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import com.wizz.card_management.service.IssuanceResult;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardCreateServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardProgramRepository cardProgramRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

        @Mock
        private CardIssuanceService cardIssuanceService;

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

        String requestHash =
                HashUtil.sha256("D|V|PROGRAM-001");

        when(idempotencyRecordRepository.tryClaim(
                eq("partner-001"),
                eq("KEY-001"),
                eq(requestHash)
        )).thenReturn(1);

        when(cardProgramRepository
                .findByProgramIdAndPartnerId(
                        "PROGRAM-001",
                        "partner-001"
                ))
                .thenReturn(Optional.of(program));

        IssuanceResult issuanceResult = new IssuanceResult(
                "DEV-TEST-123",
                "4111XXXXXXXX1111",
                "07/2031"
        );

        when(cardIssuanceService.issueCard(any(CreateCardRequest.class)))
                .thenReturn(issuanceResult);

        when(cardRepository.save(any(Card.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        IdempotencyRecord claimedRecord =
                new IdempotencyRecord();

        claimedRecord.setIdempotencyKey("KEY-001");
        claimedRecord.setPartnerId("partner-001");
        claimedRecord.setRequestHash(requestHash);
        claimedRecord.setStatus("IN_PROGRESS");

        when(idempotencyRecordRepository
                .findByPartnerIdAndIdempotencyKey(
                        "partner-001",
                        "KEY-001"
                ))
                .thenReturn(Optional.of(claimedRecord));

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

        String requestHash =
                HashUtil.sha256("D|V|PROGRAM-001");

        when(idempotencyRecordRepository.tryClaim(
                eq("partner-001"),
                eq("KEY-002"),
                eq(requestHash)
        )).thenReturn(1);

        when(cardProgramRepository
                .findByProgramIdAndPartnerId(
                        "PROGRAM-001",
                        "partner-001"
                ))
                .thenReturn(Optional.empty());

        IdempotencyRecord claimedRecord =
                createInProgressRecord(
                        "KEY-002",
                        requestHash,
                        "partner-001"
                );

        when(idempotencyRecordRepository
                .findByPartnerIdAndIdempotencyKey(
                        "partner-001",
                        "KEY-002"
                ))
                .thenReturn(Optional.of(claimedRecord));

        when(idempotencyRecordRepository.save(
                any(IdempotencyRecord.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

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
                .save(any(IdempotencyRecord.class));
    }

    @Test
    void inactiveProgram_shouldReturnDecline() {

        CardProgram program = createProgram(
                "PROGRAM-001",
                "D",
                false
        );

        String requestHash =
                HashUtil.sha256("D|V|PROGRAM-001");

        when(idempotencyRecordRepository.tryClaim(
                eq("partner-001"),
                eq("KEY-003"),
                eq(requestHash)
        )).thenReturn(1);

        when(cardProgramRepository
                .findByProgramIdAndPartnerId(
                        "PROGRAM-001",
                        "partner-001"
                ))
                .thenReturn(Optional.of(program));

        IdempotencyRecord claimedRecord =
                createInProgressRecord(
                        "KEY-003",
                        requestHash,
                        "partner-001"
                );

        when(idempotencyRecordRepository
                .findByPartnerIdAndIdempotencyKey(
                        "partner-001",
                        "KEY-003"
                ))
                .thenReturn(Optional.of(claimedRecord));

        when(idempotencyRecordRepository.save(
                any(IdempotencyRecord.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

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
                .save(any(IdempotencyRecord.class));
    }

    @Test
    void programTypeMismatch_shouldReturnDecline() {

        CardProgram program = createProgram(
                "PROGRAM-001",
                "P",
                true
        );

        String requestHash =
                HashUtil.sha256("D|V|PROGRAM-001");

        when(idempotencyRecordRepository.tryClaim(
                eq("partner-001"),
                eq("KEY-004"),
                eq(requestHash)
        )).thenReturn(1);

        when(cardProgramRepository
                .findByProgramIdAndPartnerId(
                        "PROGRAM-001",
                        "partner-001"
                ))
                .thenReturn(Optional.of(program));

        IdempotencyRecord claimedRecord =
                createInProgressRecord(
                        "KEY-004",
                        requestHash,
                        "partner-001"
                );

        when(idempotencyRecordRepository
                .findByPartnerIdAndIdempotencyKey(
                        "partner-001",
                        "KEY-004"
                ))
                .thenReturn(Optional.of(claimedRecord));

        when(idempotencyRecordRepository.save(
                any(IdempotencyRecord.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

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
                .save(any(IdempotencyRecord.class));
    }

    @Test
    void sameIdempotencyKeyAndSameRequest_shouldReplayOriginalResponse() {

        String requestHash =
                HashUtil.sha256("D|V|PROGRAM-001");

        IdempotencyRecord record =
                createCompletedRecord(
                        "KEY-005",
                        requestHash,
                        "partner-001"
                );

        when(idempotencyRecordRepository.tryClaim(
                eq("partner-001"),
                eq("KEY-005"),
                eq(requestHash)
        )).thenReturn(0);

        when(idempotencyRecordRepository
                .findByPartnerIdAndIdempotencyKey(
                        "partner-001",
                        "KEY-005"
                ))
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
                .findByProgramIdAndPartnerId(
                        anyString(),
                        anyString()
                );
    }

    @Test
    void sameIdempotencyKeyWithDifferentRequestId_shouldReplayOriginalReferenceId() {

        String requestHash =
                HashUtil.sha256("D|V|PROGRAM-001");

        IdempotencyRecord record =
                createCompletedRecord(
                        "KEY-007",
                        requestHash,
                        "partner-001"
                );

        when(idempotencyRecordRepository.tryClaim(
                eq("partner-001"),
                eq("KEY-007"),
                eq(requestHash)
        )).thenReturn(0);

        when(idempotencyRecordRepository
                .findByPartnerIdAndIdempotencyKey(
                        "partner-001",
                        "KEY-007"
                ))
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
                .findByProgramIdAndPartnerId(
                        anyString(),
                        anyString()
                );
    }

    @Test
    void sameIdempotencyKeyAndDifferentRequest_shouldThrowConflict() {

        String existingRequestHash =
                HashUtil.sha256("D|P|PROGRAM-001");

        String currentRequestHash =
                HashUtil.sha256("D|V|PROGRAM-001");

        IdempotencyRecord record =
                createCompletedRecord(
                        "KEY-006",
                        existingRequestHash,
                        "partner-001"
                );

        when(idempotencyRecordRepository.tryClaim(
                eq("partner-001"),
                eq("KEY-006"),
                eq(currentRequestHash)
        )).thenReturn(0);

        when(idempotencyRecordRepository
                .findByPartnerIdAndIdempotencyKey(
                        "partner-001",
                        "KEY-006"
                ))
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
                .findByProgramIdAndPartnerId(
                        anyString(),
                        anyString()
                );
    }

    @Test
    void inProgressIdempotencyKey_shouldNotCreateAnotherCard() {

        String requestHash =
                HashUtil.sha256("D|V|PROGRAM-001");

        IdempotencyRecord record =
                createInProgressRecord(
                        "KEY-008",
                        requestHash,
                        "partner-001"
                );

        when(idempotencyRecordRepository.tryClaim(
                eq("partner-001"),
                eq("KEY-008"),
                eq(requestHash)
        )).thenReturn(0);

        when(idempotencyRecordRepository
                .findByPartnerIdAndIdempotencyKey(
                        "partner-001",
                        "KEY-008"
                ))
                .thenReturn(Optional.of(record));

        assertThrows(
                IllegalStateException.class,
                () -> cardCreateService.createCard(
                        request,
                        "REQ-008",
                        "KEY-008",
                        "WEB",
                        "partner-001"
                )
        );

        verify(cardRepository, never())
                .save(any(Card.class));

        verify(cardProgramRepository, never())
                .findByProgramIdAndPartnerId(
                        anyString(),
                        anyString()
                );
    }

        @Test
        void cardIssuanceFailure_shouldNotSaveCard() {

        String requestHash =
                HashUtil.sha256("D|V|PROGRAM-001");

        CardProgram program = createProgram(
                "PROGRAM-001",
                "D",
                true
        );

        when(idempotencyRecordRepository.tryClaim(
                eq("partner-001"),
                eq("KEY-009"),
                eq(requestHash)
        )).thenReturn(1);

        when(cardProgramRepository.findByProgramIdAndPartnerId(
                "PROGRAM-001",
                "partner-001"
        )).thenReturn(Optional.of(program));

        when(cardIssuanceService.issueCard(
                any(CreateCardRequest.class)
        )).thenThrow(
                new RuntimeException("Issuer unavailable")
        );

        assertThrows(
                RuntimeException.class,
                () -> cardCreateService.createCard(
                        request,
                        "REQ-009",
                        "KEY-009",
                        "WEB",
                        "partner-001"
                )
        );

        verify(cardRepository, never())
                .save(any(Card.class));
        }

    private CardProgram createProgram(
            String programId,
            String programType,
            boolean active) {

        CardProgram program =
                new CardProgram();

        program.setProgramId(programId);
        program.setPartnerId("partner-001");
        program.setProgramName("Test Program");
        program.setProgramType(programType);
        program.setActive(active);

        return program;
    }

    private IdempotencyRecord createInProgressRecord(
            String key,
            String requestHash,
            String partnerId) {

        IdempotencyRecord record =
                new IdempotencyRecord();

        record.setIdempotencyKey(key);
        record.setPartnerId(partnerId);
        record.setRequestHash(requestHash);
        record.setStatus("IN_PROGRESS");

        return record;
    }

    private IdempotencyRecord createCompletedRecord(
            String key,
            String requestHash,
            String partnerId) {

        IdempotencyRecord record =
                createInProgressRecord(
                        key,
                        requestHash,
                        partnerId
                );

        record.setCardNumber("4111XXXXXXXX1111");
        record.setExpiryDate("07/2031");
        record.setCardId("CARD-001");
        record.setReferenceId("REQ-005");
        record.setResponseCode("00");
        record.setResponseDesc(
                "Card created successfully"
        );
        record.setStatus("COMPLETED");

        return record;
    }
}