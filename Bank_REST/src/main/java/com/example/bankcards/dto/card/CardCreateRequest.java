package com.example.bankcards.dto.card;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CardCreateRequest {
    @NotNull
    private Long ownerId;

    @NotBlank
    @Pattern(regexp = "\\d{16}", message = "PAN должен содержать 16 цифр")
    private String pan;

    @NotBlank
    @Size(max = 128)
    private String holderName;

    @NotNull
    private LocalDate expiryDate;

    @DecimalMin(value = "0.00")
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
