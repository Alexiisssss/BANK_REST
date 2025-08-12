package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardStatusUpdateRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cards")
public class AdminCardController {

    private static final Logger log = LoggerFactory.getLogger(AdminCardController.class);

    private final CardService cardService;

    public AdminCardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public CardResponse createCard(@Valid @RequestBody CardCreateRequest req) {
        log.info("Admin creating new card for holder: {}", req.getHolderName());
        return cardService.create(req);
    }

    @PutMapping("/{id}/status")
    public CardResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody CardStatusUpdateRequest req) {
        log.info("Admin updating status of card {} to {}", id, req.getStatus());
        return cardService.updateStatus(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        log.warn("Admin deleting card with ID {}", id);
        cardService.delete(id);
    }

    @GetMapping
    public Page<CardResponse> listAll(
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String holderName,
            @RequestParam(required = false) @jakarta.validation.constraints.Size(max = 4) String last4,
            @RequestParam(required = false) java.time.LocalDate expFrom,
            @RequestParam(required = false) java.time.LocalDate expTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id").descending());
        log.debug("Admin requested cards list with filters: ownerId={}, status={}, holderName={}", ownerId, status, holderName);
        // ownerId == null => show ALL cards
        return cardService.list(ownerId, status, holderName, last4, expFrom, expTo, pageable);
    }

    // block card
    @PutMapping("/{id}/block")
    public CardResponse block(@PathVariable("id") Long id) {
        var req = new com.example.bankcards.dto.card.CardStatusUpdateRequest();
        req.setStatus(com.example.bankcards.entity.CardStatus.BLOCKED);
        log.warn("Admin blocking card with ID {}", id);
        return cardService.updateStatus(id, req);
    }

    // activate card
    @PutMapping("/{id}/activate")
    public CardResponse activate(@PathVariable("id") Long id) {
        var req = new com.example.bankcards.dto.card.CardStatusUpdateRequest();
        req.setStatus(com.example.bankcards.entity.CardStatus.ACTIVE);
        log.info("Admin activating card with ID {}", id);
        return cardService.updateStatus(id, req);
    }

    // get any card by id
    @GetMapping("/{id}")
    public CardResponse getById(@PathVariable("id") Long id) {
        var card = cardService.findById(id)
                .orElseThrow(() -> new com.example.bankcards.exception.NotFoundException("Card not found"));
        log.debug("Admin fetching card by ID {}", id);
        return com.example.bankcards.mapper.CardMapper.toResponse(card);
    }
}
