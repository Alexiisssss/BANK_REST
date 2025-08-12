package com.example.bankcards.dto.transfer;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TransferRequest {
    @NotNull(message = "From card id is required.")
    private Long fromCardId;

    @NotNull(message = "To card id is required.")
    private Long toCardId;

    @NotNull(message = "Amount is required.")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01.")
    @Digits(integer = 18, fraction = 2, message = "Amount precision is invalid.")
    private BigDecimal amount;

    @Size(max = 255, message = "Description must be at most 255 characters.")
    private String description;

    public Long getFromCardId() {
        return fromCardId;
    }

    public void setFromCardId(Long fromCardId) {
        this.fromCardId = fromCardId;
    }

    public Long getToCardId() {
        return toCardId;
    }

    public void setToCardId(Long toCardId) {
        this.toCardId = toCardId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
