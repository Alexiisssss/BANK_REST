package com.example.bankcards.dto.card;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CardCreateRequest {
    @NotNull(message = "Owner id is required.")
    private Long ownerId;

    @NotBlank(message = "PAN is required.")
    @Pattern(regexp = "\\d{16}", message = "PAN must contain exactly 16 digits.")
    private String pan;

    @NotBlank(message = "Card holder name is required.")
    @Size(max = 128, message = "Card holder name must be at most 128 characters.")
    private String holderName;

    @NotNull(message = "Expiry date is required.")
    @Future(message = "Expiry date must be in the future.")
    private LocalDate expiryDate;

    @DecimalMin(value = "0.00", message = "Initial balance must be >= 0.00.")
    @Digits(integer = 18, fraction = 2, message = "Initial balance precision is invalid.")
    private BigDecimal initialBalance = BigDecimal.ZERO;

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
}
