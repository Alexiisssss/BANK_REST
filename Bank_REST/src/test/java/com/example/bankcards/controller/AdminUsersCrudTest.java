package com.example.bankcards.controller;

import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.user.UserBlockRequest;
import com.example.bankcards.dto.user.UserCreateRequest;
import com.example.bankcards.dto.user.UserRoleUpdateRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminUsersCrudTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setup() throws Exception {
        User admin = userRepository.findByUsername("admin").orElseGet(User::new);
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(User.Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);

        adminToken = loginAndGetToken("admin", "admin123");
    }

    @Test
    void create_changeRole_block_unblock_flow() throws Exception {
        // create
        UserCreateRequest create = new UserCreateRequest();
        create.setUsername("u_admin_flow");
        create.setPassword("pwd123");
        create.setRole("USER");

        String createdIdJson = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(createdIdJson).asLong();
        User created = userRepository.findById(userId).orElseThrow();
        assertThat(created.getUsername()).isEqualTo("u_admin_flow");
        assertThat(created.getRole()).isEqualTo(User.Role.USER);
        assertThat(created.isEnabled()).isTrue();

        // change role -> ADMIN
        UserRoleUpdateRequest roleReq = new UserRoleUpdateRequest();
        roleReq.setRole("ADMIN");
        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isOk());
        assertThat(userRepository.findById(userId).orElseThrow().getRole())
                .isEqualTo(User.Role.ADMIN);

        // block (disabled = true)
        UserBlockRequest blockReq = new UserBlockRequest();
        blockReq.setDisabled(true);
        mockMvc.perform(put("/api/admin/users/{id}/block", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockReq)))
                .andExpect(status().isOk());
        assertThat(userRepository.findById(userId).orElseThrow().isEnabled())
                .isFalse();

        // unblock (disabled = false)
        blockReq.setDisabled(false);
        mockMvc.perform(put("/api/admin/users/{id}/block", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockReq)))
                .andExpect(status().isOk());
        assertThat(userRepository.findById(userId).orElseThrow().isEnabled())
                .isTrue();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);

        String resp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(resp).get("token").asText();
    }
}
