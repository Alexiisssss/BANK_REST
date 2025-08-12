package com.example.bankcards.controller;

import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminCardsCrudTest {

    private static final Logger log = LoggerFactory.getLogger(AdminCardsCrudTest.class);

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private String adminToken;
    private Long userId;

    @BeforeEach
    void setUp() throws Exception {
        log.info("=== Test setup started ===");

        // Ensure admin exists
        log.info("Ensuring admin user exists...");
        User admin = userRepository.findByUsername("admin").orElseGet(User::new);
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(User.Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);

        // Ensure regular test user exists
        log.info("Ensuring CRUD test user exists...");
        userRepository.findByUsername("crud_user").ifPresentOrElse(u -> {
            userId = u.getId();
            log.info("User 'crud_user' already exists with ID {}", userId);
        }, () -> {
            RegisterRequest reg = new RegisterRequest();
            reg.setUsername("crud_user");
            reg.setPassword("pass123");
            try {
                String regResp = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reg)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
                userId = objectMapper.readTree(regResp).get("id").asLong();
                log.info("Registered new user 'crud_user' with ID {}", userId);
            } catch (Exception e) {
                log.error("Error registering user 'crud_user'", e);
                throw new RuntimeException(e);
            }
        });

        // Login as admin
        adminToken = loginAndGetToken("admin", "admin123");
        log.info("Admin token obtained: {}", adminToken);
    }

    @Test
    void admin_card_crud_flow() throws Exception {
        log.info("=== Running admin_card_crud_flow test ===");

        // Create a new card for the test user
        Long cardId = createCardAsAdmin(userId, "4111111111111111", "crud_user");
        log.info("Created card ID {} for user {}", cardId, userId);

        // Verify card is listed in admin view
        String list = mockMvc.perform(get("/api/admin/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        log.info("Card list response: {}", list);
        assertThat(objectMapper.readTree(list).get("content").toString())
                .contains("\"id\":" + cardId);

        // Block the card
        String blocked = mockMvc.perform(put("/api/admin/cards/{id}/block", cardId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        log.info("Blocked card response: {}", blocked);
        assertThat(objectMapper.readTree(blocked).get("status").asText()).isEqualTo("BLOCKED");

        // Activate the card again
        String activated = mockMvc.perform(put("/api/admin/cards/{id}/activate", cardId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        log.info("Activated card response: {}", activated);
        assertThat(objectMapper.readTree(activated).get("status").asText()).isEqualTo("ACTIVE");

        // Delete the card
        mockMvc.perform(delete("/api/admin/cards/{id}", cardId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        log.info("Deleted card ID {}", cardId);

        // Ensure deleted card is not accessible for the user
        String userToken = loginAndGetToken("crud_user", "pass123");
        mockMvc.perform(get("/api/cards/{id}", cardId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
        log.info("Verified deleted card is not accessible for the user");
    }

    @Test
    void admin_list_with_filters() throws Exception {
        log.info("=== Running admin_list_with_filters test ===");

        // Create another test user
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("filter_user");
        reg.setPassword("pass123");
        String regResp = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long secondUserId = objectMapper.readTree(regResp).get("id").asLong();
        log.info("Created second test user 'filter_user' with ID {}", secondUserId);

        // Create two cards for different users
        Long cardId1 = createCardAsAdmin(userId, "4111111111111234", "crud_user"); // last4: 1234
        Long cardId2 = createCardAsAdmin(secondUserId, "5555555555559876", "filter_user"); // last4: 9876
        log.info("Created cards: {} (user {}) and {} (user {})", cardId1, userId, cardId2, secondUserId);

        // Block the second card
        mockMvc.perform(put("/api/admin/cards/{id}/block", cardId2)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        log.info("Blocked card ID {}", cardId2);

        // Filter by ownerId
        String listByOwner = mockMvc.perform(get("/api/admin/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("ownerId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        log.info("Filter by ownerId={} response: {}", userId, listByOwner);
        assertThat(listByOwner).contains("\"id\":" + cardId1)
                .doesNotContain("\"id\":" + cardId2);

        // Filter by status
        String listBlocked = mockMvc.perform(get("/api/admin/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "BLOCKED"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        log.info("Filter by status=BLOCKED response: {}", listBlocked);
        assertThat(listBlocked).contains("\"id\":" + cardId2)
                .doesNotContain("\"id\":" + cardId1);

        // Filter by last4 digits
        String listByLast4 = mockMvc.perform(get("/api/admin/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("last4", "1234"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        log.info("Filter by last4=1234 response: {}", listByLast4);
        assertThat(listByLast4).contains("\"id\":" + cardId1);
    }

    // === Helper methods ===

    /**
     * Log in with given credentials and return JWT token
     */
    private String loginAndGetToken(String username, String password) throws Exception {
        log.info("Logging in as user '{}'", username);
        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);

        String resp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(resp).get("token").asText();
        log.info("Login successful for '{}', token: {}", username, token);
        return token;
    }

    /**
     * Create a card as admin for the specified user
     */
    private Long createCardAsAdmin(Long ownerId, String pan, String holderName) throws Exception {
        log.info("Creating card for ownerId={}, holderName={}", ownerId, holderName);
        com.example.bankcards.dto.card.CardCreateRequest req = new com.example.bankcards.dto.card.CardCreateRequest();
        req.setOwnerId(ownerId);
        req.setPan(pan);
        req.setHolderName(holderName);
        req.setExpiryDate(LocalDate.now().plusYears(2));
        req.setInitialBalance(new BigDecimal("1000"));

        String resp = mockMvc.perform(post("/api/admin/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long cardId = objectMapper.readTree(resp).get("id").asLong();
        log.info("Created card ID {}", cardId);
        return cardId;
    }
}
