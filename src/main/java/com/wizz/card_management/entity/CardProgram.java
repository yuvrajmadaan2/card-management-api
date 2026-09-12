package com.wizz.card_management.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "card_programs")
public class CardProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String programId;

    @Column(name = "partner_id", nullable = false, updatable = false)
    private String partnerId;

    @Column(nullable = false)
    private String programName;

    @Column(nullable = false)
    private String programType;

    @Column(nullable = false)
    private boolean active;

    public Long getId() {
        return id;
    }

    public String getProgramId() {
        return programId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getProgramType() {
        return programType;
    }

    public void setProgramType(String programType) {
        this.programType = programType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}