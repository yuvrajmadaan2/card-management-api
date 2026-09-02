package com.wizz.card_management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;


public class TxnControlsSetRequest {

    @NotBlank(message = "cardId must not be blank")
    @Size(max = 20, message = "cardId must not exceed 20 characters")
    private String cardId;

    @Size(max = 20, message = "customerId must not exceed 20 characters")
    private String customerId;

    @Valid
    @NotNull(message = "channel must not be null")
    private ControlUpdate channel;

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public ControlUpdate getChannel() {
        return channel;
    }

    public void setChannel(ControlUpdate channel) {
        this.channel = channel;
    }

    public static class ControlUpdate {

        @NotBlank(message = "channelType must not be blank")
        private String channelType;

        @NotNull(message = "allowed must not be null")
        private Boolean allowed;

        public String getChannelType() {
            return channelType;
        }

        public void setChannelType(String channelType) {
            this.channelType = channelType;
        }

        public Boolean getAllowed() {
            return allowed;
        }

        public void setAllowed(Boolean allowed) {
            this.allowed = allowed;
        }
    }
}