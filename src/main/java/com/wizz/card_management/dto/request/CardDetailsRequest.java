package com.wizz.card_management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CardDetailsRequest {

    @NotNull(message = "cards is mandatory")
    @Size(
            min = 1,
            max = 10,
            message = "cards must contain between 1 and 10 items"
    )
    @Valid
    private List<CardRef> cards;

    @Size(
            max = 20,
            message = "customerId must not exceed 20 characters"
    )
    private String customerId;

    public List<CardRef> getCards() {
        return cards;
    }

    public void setCards(List<CardRef> cards) {
        this.cards = cards;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public static class CardRef {

        @NotBlank(message = "cardId is mandatory")
        @Size(
                max = 20,
                message = "cardId must not exceed 20 characters"
        )
        private String cardId;

        public String getCardId() {
            return cardId;
        }

        public void setCardId(String cardId) {
            this.cardId = cardId;
        }
    }
}