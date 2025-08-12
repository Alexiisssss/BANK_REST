package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/cards")
public class CardController {
    private final CardService cardService;
    private final UserRepository userRepository;

    public CardController(CardService cardService, UserRepository userRepository) {
        this.cardService = cardService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}")
    public CardResponse getCard(@PathVariable("id") Long id, Authentication auth) {
        Long userId = resolveUserId(auth);
        log.debug("getCard: userId={}, cardId={}", userId, id);

        var card = cardService.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new com.example.bankcards.exception.NotFoundException("Card not found"));

        return com.example.bankcards.mapper.CardMapper.toResponse(card);
    }

    @GetMapping
    public Page<CardResponse> listCards(
            Authentication auth,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String holderName,
            @RequestParam(required = false) @Size(max = 4) String last4,
            @RequestParam(required = false) LocalDate expFrom,
            @RequestParam(required = false) LocalDate expTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = resolveUserId(auth);

        // normalize inputs a bit (avoid NPEs and accidental spaces)
        String holder = (holderName == null ? null : holderName.trim());
        String last4norm = (last4 == null ? null : last4.trim());

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        log.debug("listCards: userId={}, status={}, holderName='{}', last4='{}', expFrom={}, expTo={}, page={}, size={}",
                userId, status, holder, last4norm, expFrom, expTo, page, size);

        return cardService.list(userId, status, holder, last4norm, expFrom, expTo, pageable);
    }

    private Long resolveUserId(Authentication auth) {
        if (auth == null) {
            log.warn("resolveUserId: auth is null");
            throw new com.example.bankcards.exception.UnauthorizedException("Unknown user (no Authentication)");
        }

        Object details = auth.getDetails();
        if (details instanceof Long l) {
            log.debug("resolveUserId: got userId from details={}", l);
            return l;
        } else {
            if (details != null) {
                log.info("resolveUserId: details present but not Long ({}). Falling back to username.",
                        details.getClass().getName());
            } else {
                log.info("resolveUserId: details is null. Falling back to username.");
            }
        }

        String username = auth.getName();
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> {
                    log.warn("resolveUserId: user not found by username='{}'", username);
                    return new com.example.bankcards.exception.UnauthorizedException("User not found");
                });
    }
}
