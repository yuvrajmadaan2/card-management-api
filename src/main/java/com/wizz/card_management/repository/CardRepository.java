package com.wizz.card_management.repository;

import com.wizz.card_management.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByCardId(String cardId);

}