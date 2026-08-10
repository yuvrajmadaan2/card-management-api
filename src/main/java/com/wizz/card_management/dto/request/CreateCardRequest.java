package com.wizz.card_management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CreateCardRequest {

    @NotNull(message = "card is mandatory")
    @Valid
    private CardPayload card;

    public CardPayload getCard() {
        return card;
    }

    public void setCard(CardPayload card) {
        this.card = card;
    }

    public static class CardPayload {

        @NotBlank(message = "cardProgramType is mandatory")
        @Pattern(
                regexp = "D|P|C",
                message = "cardProgramType must be D, P or C"
        )
        private String cardProgramType;

        @NotBlank(message = "cardType is mandatory")
        @Pattern(
                regexp = "V|P",
                message = "cardType must be V or P"
        )
        private String cardType;

        @NotBlank(message = "cardProgramId is mandatory")
        private String cardProgramId;

        public String getCardProgramType() {
            return cardProgramType;
        }

        public void setCardProgramType(String cardProgramType) {
            this.cardProgramType = cardProgramType;
        }

        public String getCardType() {
            return cardType;
        }

        public void setCardType(String cardType) {
            this.cardType = cardType;
        }

        public String getCardProgramId() {
            return cardProgramId;
        }

        public void setCardProgramId(String cardProgramId) {
            this.cardProgramId = cardProgramId;
        }
    }
}