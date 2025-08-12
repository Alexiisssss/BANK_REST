package com.example.bankcards.controller;

import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BankCardsIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(BankCardsIntegrationTest.class);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private Long userId;
    private Long cardId1;
    private Long cardId2;

    @BeforeEach
    void setUp() throws Exception {
        // 0) Ensure admin exists directly via repository
        User admin = userRepository.findByUsername("admin").orElseGet(User::new);
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(User.Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);

        // 1) Register a regular user via API
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("Aleks Vol");
        reg.setPassword("pass123");

        String regResp = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        userId = objectMapper.readTree(regResp).get("id").asLong();

        assertThat(userRepository.findByUsername("Aleks Vol")).isPresent();
        log.info("User 'Aleks Vol' has been successfully registered and found in the database");

        // 2) Login as admin (for creating cards)
        adminToken = loginAndGetToken("admin", "admin123");

        // 3) Login as user (for transfers/reading)
        userToken = loginAndGetToken("Aleks Vol", "pass123");

        // 4) Create two cards for the user (via admin endpoint)
        cardId1 = createCardAsAdmin(userId, "1111222233334444");
        cardId2 = createCardAsAdmin(userId, "5555666677778888");
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

    private Long createCardAsAdmin(Long ownerId, String pan) throws Exception {
        CardCreateRequest req = new CardCreateRequest();
        req.setOwnerId(ownerId);
        req.setPan(pan);
        req.setHolderName("Aleks Vol");
        req.setExpiryDate(LocalDate.now().plusYears(2));
        req.setInitialBalance(new BigDecimal("500"));

        String resp = mockMvc.perform(post("/api/admin/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(resp).get("id").asLong();
    }

    @Test
    void fullCycleTest() throws Exception {
        // Transfer 200 between own cards (user token)
        TransferRequest transfer = new TransferRequest();
        transfer.setFromCardId(cardId1);
        transfer.setToCardId(cardId2);
        transfer.setAmount(new BigDecimal("200"));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isOk());

        // Check balances for created card IDs
        String c1 = mockMvc.perform(get("/api/cards/" + cardId1)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String c2 = mockMvc.perform(get("/api/cards/" + cardId2)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode n1 = objectMapper.readTree(c1);
        JsonNode n2 = objectMapper.readTree(c2);

        assertThat(new BigDecimal(n1.get("balance").asText())).isEqualByComparingTo("300");
        assertThat(new BigDecimal(n2.get("balance").asText())).isEqualByComparingTo("700");
    }
}
