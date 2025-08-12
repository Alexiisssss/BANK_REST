package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.NotNull;

public class CardStatusUpdateRequest {
    @NotNull(message = "Status is required.")
    private CardStatus status;

    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }
}
