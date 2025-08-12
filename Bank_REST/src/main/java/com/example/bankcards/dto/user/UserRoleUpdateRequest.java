package com.example.bankcards.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserRoleUpdateRequest {
    @NotBlank(message = "Role must not be blank.")
    @Pattern(regexp = "ADMIN|USER", message = "Role must be either ADMIN or USER.")
    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
