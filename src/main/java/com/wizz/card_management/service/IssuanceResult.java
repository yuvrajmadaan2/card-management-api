package com.wizz.card_management.service;

public record IssuanceResult(
        String processorReference,
        String maskedCardNumber,
        String expiryDate
) {
}