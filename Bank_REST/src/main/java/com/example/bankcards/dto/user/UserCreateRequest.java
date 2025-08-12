package com.example.bankcards.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserCreateRequest {
    @NotBlank(message = "Username must not be blank.")
    private String username;

    @NotBlank(message = "Password must not be blank.")
    private String password;

    @Pattern(regexp = "ADMIN|USER", message = "Role must be either ADMIN or USER.")
    private String role = "USER";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
