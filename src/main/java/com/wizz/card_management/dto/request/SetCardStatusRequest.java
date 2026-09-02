package com.wizz.card_management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Pattern;

public class SetCardStatusRequest {

    @NotNull(message = "card is mandatory")
    @Valid
    private CardStatusPayload card;

    public CardStatusPayload getCard() {
        return card;
    }

    public void setCard(CardStatusPayload card) {
        this.card = card;
    }

    public static class CardStatusPayload {

        @NotBlank(message = "cardId is mandatory")
        @Size(
                max = 20,
                message = "cardId must not exceed 20 characters"
        )
        private String cardId;

        @NotBlank(message = "statusCode is mandatory")
        private String statusCode;

        @Size(max = 10)
        private String reasonCode;

        @Size(
                max = 200,
                message = "remarks must not exceed 200 characters"
        )
        private String remarks;

        public String getCardId() {
            return cardId;
        }

        public void setCardId(String cardId) {
            this.cardId = cardId;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(String statusCode) {
            this.statusCode = statusCode;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public void setReasonCode(String reasonCode) {
            this.reasonCode = reasonCode;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
    }
}