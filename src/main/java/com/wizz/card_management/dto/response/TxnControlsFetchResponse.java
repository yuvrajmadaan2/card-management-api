package com.wizz.card_management.dto.response;

import java.util.List;

public class TxnControlsFetchResponse {

    private String referenceId;
    private String responseCode;
    private String responseDesc;
    private List<CardControls> channels;

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

    public List<CardControls> getChannels() {
        return channels;
    }

    public void setChannels(List<CardControls> channels) {
        this.channels = channels;
    }

    public static class CardControls {

        private String cardId;
        private List<ControlRow> lists;

        public String getCardId() {
            return cardId;
        }

        public void setCardId(String cardId) {
            this.cardId = cardId;
        }

        public List<ControlRow> getLists() {
            return lists;
        }

        public void setLists(List<ControlRow> lists) {
            this.lists = lists;
        }
    }

    public static class ControlRow {

        private String channelType;
        private Boolean allowed;
        private Boolean editable;

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

        public Boolean getEditable() {
            return editable;
        }

        public void setEditable(Boolean editable) {
            this.editable = editable;
        }
    }
}