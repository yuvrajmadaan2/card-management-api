package com.wizz.card_management.repository;

import com.wizz.card_management.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    Optional<IdempotencyRecord> findByPartnerIdAndIdempotencyKey(
            String partnerId,
            String idempotencyKey
    );

        @Modifying
        @Query(value = """
                INSERT INTO idempotency_records
                (idempotency_key, partner_id, request_hash, status)
                VALUES (:idempotencyKey, :partnerId, :requestHash, 'IN_PROGRESS')
                ON CONFLICT (partner_id, idempotency_key) DO NOTHING
                """, nativeQuery = true)
        int tryClaim(
                @Param("partnerId") String partnerId,
                @Param("idempotencyKey") String idempotencyKey,
                @Param("requestHash") String requestHash
        );
}