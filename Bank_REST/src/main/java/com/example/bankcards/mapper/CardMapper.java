package com.example.bankcards.mapper;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;

public final class CardMapper {
    private CardMapper() {
    }

    public static CardResponse toResponse(Card c) {
        CardResponse dto = new CardResponse();
        dto.setId(c.getId());
        dto.setOwnerId(c.getOwner().getId());
        dto.setHolderName(c.getHolderName());
        dto.setPanMask(c.getPanMask());
        dto.setExpiryDate(c.getExpiryDate());
        dto.setStatus(c.getStatus());
        dto.setBalance(c.getBalance());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }
}
