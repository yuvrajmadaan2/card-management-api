package com.wizz.card_management.dto.response;

import java.util.List;

public class CardDetailsResponse {

    private String referenceId;
    private String responseCode;
    private String responseDesc;
    private List<CardDetail> cards;

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseDesc() {
        return responseDesc;
    }

    public void setResponseDesc(String responseDesc) {
        this.responseDesc = responseDesc;
    }

    public List<CardDetail> getCards() {
        return cards;
    }

    public void setCards(List<CardDetail> cards) {
        this.cards = cards;
    }

    public static class CardDetail {

        private String cardId;
        private String cardProgramType;
        private String cardType;
        private String cardProgramId;
        private String cardProgramName;
        private String cardNumber;
        private String expiryDate;
        private String cardStatus;
        private String cardStatusDesc;

        public String getCardId() {
            return cardId;
        }

        public void setCardId(String cardId) {
            this.cardId = cardId;
        }

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

        public String getCardProgramName() {
            return cardProgramName;
        }

        public void setCardProgramName(String cardProgramName) {
            this.cardProgramName = cardProgramName;
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public void setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
        }

        public String getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(String expiryDate) {
            this.expiryDate = expiryDate;
        }

        public String getCardStatus() {
            return cardStatus;
        }

        public void setCardStatus(String cardStatus) {
            this.cardStatus = cardStatus;
        }

        public String getCardStatusDesc() {
            return cardStatusDesc;
        }

        public void setCardStatusDesc(String cardStatusDesc) {
            this.cardStatusDesc = cardStatusDesc;
        }
    }
}