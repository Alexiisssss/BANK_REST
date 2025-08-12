package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardStatusUpdateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.repository.spec.CardSpecs;
import com.example.bankcards.util.MaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
public class CardService {

    private final CardRepository cardRepo;
    private final UserRepository userRepo;
    private final CryptoService crypto;

    public CardService(CardRepository cardRepo, UserRepository userRepo, CryptoService crypto) {
        this.cardRepo = cardRepo;
        this.userRepo = userRepo;
        this.crypto = crypto;
    }

    @Transactional
    public CardResponse create(CardCreateRequest req) {
        log.info("[{}] Creating card for user ID {}", MDC.get("reqId"), req.getOwnerId());

        User owner = userRepo.findById(req.getOwnerId())
                .orElseThrow(() -> {
                    log.warn("[{}] Owner with ID {} not found", MDC.get("reqId"), req.getOwnerId());
                    return new NotFoundException("Owner not found");
                });

        if (req.getExpiryDate().isBefore(LocalDate.now())) {
            log.warn("[{}] Attempt to create card with past expiry date: {}", MDC.get("reqId"), req.getExpiryDate());
            throw new BusinessException("Expiry date is in the past");
        }

        String panMask = MaskUtils.maskPan(req.getPan());
        String encPan = crypto.encrypt(req.getPan());

        Card c = new Card();
        c.setOwner(owner);
        c.setEncryptedPan(encPan);
        c.setPanMask(panMask);
        c.setHolderName(req.getHolderName());
        c.setExpiryDate(req.getExpiryDate());
        c.setStatus(CardStatus.ACTIVE);
        c.setBalance(req.getInitialBalance() == null ? BigDecimal.ZERO : req.getInitialBalance());

        cardRepo.save(c);

        log.info("[{}] Card created successfully: {}", MDC.get("reqId"), panMask);
        return CardMapper.toResponse(c);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> list(Long ownerId,
                                   CardStatus status,
                                   String holderSearch,
                                   String last4,
                                   LocalDate expFrom,
                                   LocalDate expTo,
                                   Pageable pageable) {
        Specification<Card> spec = Specification.allOf(
                CardSpecs.ownerId(ownerId),
                CardSpecs.status(status),
                CardSpecs.holderNameContains(holderSearch),
                CardSpecs.panMaskEndsWith(last4),
                CardSpecs.expiryBetween(expFrom, expTo)
        );

        return cardRepo.findAll(spec, pageable).map(CardMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Card getByIdOwned(Long id, Long requesterUserId) {
        Card c = cardRepo.findById(id).orElseThrow(() -> new NotFoundException("Card not found"));
        if (!c.getOwner().getId().equals(requesterUserId)) {
            throw new BusinessException("Card does not belong to user");
        }
        return c;
    }

    @Transactional
    public CardResponse updateStatus(Long cardId, CardStatusUpdateRequest req) {
        log.info("[{}] Updating card status. Card ID: {}, New status: {}", MDC.get("reqId"), cardId, req.getStatus());

        Card c = cardRepo.findById(cardId).orElseThrow(() -> {
            log.warn("[{}] Card with ID {} not found for status update", MDC.get("reqId"), cardId);
            return new NotFoundException("Card not found");
        });

        c.setStatus(req.getStatus());

        log.info("[{}] Card ID {} status updated to {}", MDC.get("reqId"), cardId, req.getStatus());
        return CardMapper.toResponse(c);
    }

    @Transactional
    public void delete(Long id) {
        log.info("[{}] Deleting card with ID {}", MDC.get("reqId"), id);

        if (!cardRepo.existsById(id)) {
            log.warn("[{}] Card with ID {} not found for deletion", MDC.get("reqId"), id);
            throw new NotFoundException("Card not found");
        }

        cardRepo.deleteById(id);
        log.info("[{}] Card ID {} deleted successfully", MDC.get("reqId"), id);
    }

    public Optional<Card> findByIdAndUserId(Long id, Long userId) {
        return cardRepo.findByIdAndOwner_Id(id, userId);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Card> findById(Long id) {
        return cardRepo.findById(id);
    }
}
