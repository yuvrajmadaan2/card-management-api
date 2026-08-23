package com.wizz.card_management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class TxnControlsFetchRequest {

    @NotEmpty(message = "cards must not be empty")
    @Size(max = 10, message = "Maximum 10 cards are allowed")
    @Valid
    private List<CardRef> cards;

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

        @NotBlank(message = "cardId must not be blank")
        private String cardId;

        public String getCardId() {
            return cardId;
        }

        public void setCardId(String cardId) {
            this.cardId = cardId;
        }
    }
}