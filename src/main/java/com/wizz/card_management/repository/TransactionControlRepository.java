package com.wizz.card_management.repository;

import com.wizz.card_management.entity.TransactionControl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionControlRepository
        extends JpaRepository<TransactionControl, Long> {

    Optional<TransactionControl> findByCardIdAndChannelType(
            String cardId,
            String channelType
    );

    List<TransactionControl> findByCardId(
            String cardId
    );
}
