package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.*;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final CardRepository cardRepo;
    private final TransferRepository transferRepo;

    public TransferService(CardRepository cardRepo, TransferRepository transferRepo) {
        this.cardRepo = cardRepo;
        this.transferRepo = transferRepo;
    }

    @Transactional
    public TransferResponse transfer(Long userId, TransferRequest req) {
        log.debug("Starting transfer: userId={}, request={}", userId, req);

        if (req.getFromCardId().equals(req.getToCardId())) {
            log.warn("Transfer failed: source and destination card IDs are the same: {}", req.getFromCardId());
            throw new BusinessException("Cannot transfer to the same card");
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Transfer failed: invalid amount {}", req.getAmount());
            throw new BusinessException("Amount must be greater than 0");
        }

        Long a = req.getFromCardId();
        Long b = req.getToCardId();

        // fixed lock order
        Long firstId = a < b ? a : b;
        Long secondId = a < b ? b : a;

        log.debug("Locking cards in order: firstId={}, secondId={}", firstId, secondId);

        Card first = cardRepo.findByIdAndOwner_IdForUpdate(firstId, userId)
                .orElseThrow(() -> {
                    log.error("Card {} not found or does not belong to user {}", firstId, userId);
                    return new NotFoundException("Card not found or does not belong to user");
                });
        Card second = cardRepo.findByIdAndOwner_IdForUpdate(secondId, userId)
                .orElseThrow(() -> {
                    log.error("Card {} not found or does not belong to user {}", secondId, userId);
                    return new NotFoundException("Card not found or does not belong to user");
                });

        Card from = (first.getId().equals(a)) ? first : second;
        Card to = (from == first) ? second : first;

        log.debug("Source card: {}, Destination card: {}", from.getId(), to.getId());

        if (from.getStatus() != CardStatus.ACTIVE) {
            log.warn("Transfer failed: source card {} is not active", from.getId());
            throw new BusinessException("Source card is not available");
        }
        if (to.getStatus() != CardStatus.ACTIVE) {
            log.warn("Transfer failed: destination card {} is not active", to.getId());
            throw new BusinessException("Destination card is not available");
        }

        if (from.getBalance().compareTo(req.getAmount()) < 0) {
            log.warn("Transfer failed: insufficient funds on card {} (balance={}, required={})",
                    from.getId(), from.getBalance(), req.getAmount());
            throw new BusinessException("Insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(req.getAmount()));
        to.setBalance(to.getBalance().add(req.getAmount()));
        log.info("Balances updated: fromCardId={}, newBalance={}, toCardId={}, newBalance={}",
                from.getId(), from.getBalance(), to.getId(), to.getBalance());

        Transfer tr = new Transfer();
        tr.setFromCard(from);
        tr.setToCard(to);
        tr.setAmount(req.getAmount());
        tr.setStatus(TransferStatus.SUCCESS);
        tr.setDescription(req.getDescription());
        tr.setCreatedAt(OffsetDateTime.now());
        transferRepo.save(tr);

        log.info("Transfer successful: transferId={}, fromCardId={}, toCardId={}, amount={}",
                tr.getId(), from.getId(), to.getId(), tr.getAmount());

        TransferResponse resp = new TransferResponse();
        resp.setId(tr.getId());
        resp.setFromCardId(from.getId());
        resp.setToCardId(to.getId());
        resp.setAmount(tr.getAmount());
        resp.setStatus(tr.getStatus());
        resp.setDescription(tr.getDescription());
        resp.setCreatedAt(tr.getCreatedAt());

        log.debug("Returning transfer response: {}", resp);
        return resp;
    }
}
