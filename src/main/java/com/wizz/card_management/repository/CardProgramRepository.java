package com.wizz.card_management.repository;

import com.wizz.card_management.entity.CardProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardProgramRepository extends JpaRepository<CardProgram, Long> {

    Optional<CardProgram> findByProgramId(String programId);
}