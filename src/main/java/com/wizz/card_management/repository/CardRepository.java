package com.wizz.card_management.repository;

import com.wizz.card_management.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByCardId(String cardId);

    Optional<Card> findByCardIdAndPartnerId(String cardId, String partnerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c WHERE c.cardId = :cardId AND c.partnerId = :partnerId")
    Optional<Card> findByCardIdAndPartnerIdForUpdate(
            @Param("cardId") String cardId,
            @Param("partnerId") String partnerId
    );

}