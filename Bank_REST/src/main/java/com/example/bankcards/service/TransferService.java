package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.*;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferService {

    private final CardRepository cardRepo;
    private final TransferRepository transferRepo;

    public TransferService(CardRepository cardRepo, TransferRepository transferRepo) {
        this.cardRepo = cardRepo;
        this.transferRepo = transferRepo;
    }

    /**
     * Перевод между своими картами (оба принадлежат одному пользователю).
     *
     * @param requesterUserId — id пользователя, от имени которого производится операция
     */
    @Transactional
    public TransferResponse transfer(Long requesterUserId, TransferRequest req) {
        if (req.getFromCardId().equals(req.getToCardId())) {
            throw new BusinessException("Cannot transfer to the same card");
        }

        Card from = cardRepo.findByIdForUpdate(req.getFromCardId())
                .orElseThrow(() -> new NotFoundException("Source card not found"));
        Card to = cardRepo.findByIdForUpdate(req.getToCardId())
                .orElseThrow(() -> new NotFoundException("Target card not found"));

        // проверка владения
        if (!from.getOwner().getId().equals(requesterUserId) || !to.getOwner().getId().equals(requesterUserId)) {
            throw new BusinessException("Both cards must belong to the user");
        }

        // статусы
        if (from.getStatus() != CardStatus.ACTIVE || to.getStatus() != CardStatus.ACTIVE) {
            throw new BusinessException("Card is not active");
        }

        // срок действия
        if (from.getExpiryDate().isBefore(java.time.LocalDate.now()) ||
                to.getExpiryDate().isBefore(java.time.LocalDate.now())) {
            throw new BusinessException("Card expired");
        }

        BigDecimal amount = req.getAmount();
        if (from.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        // сохраняем карты
        cardRepo.save(from);
        cardRepo.save(to);

        // история перевода
        Transfer t = new Transfer();
        t.setFromCard(from);
        t.setToCard(to);
        t.setAmount(amount);
        t.setStatus(TransferStatus.SUCCESS);
        t.setDescription(req.getDescription());
        transferRepo.save(t);

        TransferResponse resp = new TransferResponse();
        resp.setId(t.getId());
        resp.setFromCardId(from.getId());
        resp.setToCardId(to.getId());
        resp.setAmount(amount);
        resp.setStatus(t.getStatus());
        resp.setDescription(t.getDescription());
        resp.setCreatedAt(t.getCreatedAt());
        return resp;
    }
}
