package com.example.bankcards.dto.user;

public class UserBlockRequest {
    // true = disable user, false = enable
    private boolean disabled;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
