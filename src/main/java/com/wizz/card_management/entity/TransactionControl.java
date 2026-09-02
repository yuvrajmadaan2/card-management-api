package com.wizz.card_management.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "transaction_controls",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "card_id",
                                "channel_type"
                        }
                )
        }
)
public class TransactionControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private String cardId;

    @Column(name = "channel_type", nullable = false)
    private String channelType;

    @Column(nullable = false)
    private boolean allowed;

    @Column(nullable = false)
    private boolean editable;

    public Long getId() {
        return id;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}