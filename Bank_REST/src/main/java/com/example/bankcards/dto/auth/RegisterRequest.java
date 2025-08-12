package com.example.bankcards.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "Username must not be blank.")
    @Size(min = 3, max = 64, message = "Username length must be between 3 and 64 characters.")
    private String username;
    @NotBlank(message = "Password must not be blank.")
    @Size(min = 6, max = 128, message = "Password length must be between 6 and 128 characters.")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
