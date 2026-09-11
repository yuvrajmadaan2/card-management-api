package com.wizz.card_management.dto.response;

public class SetCardStatusResponse {

    private String cardNumber;
    private String cardProgramName;
    private String customerId;
    private String referenceId;
    private String responseCode;
    private String responseDesc;

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardProgramName() {
        return cardProgramName;
    }

    public void setCardProgramName(String cardProgramName) {
        this.cardProgramName = cardProgramName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

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
}