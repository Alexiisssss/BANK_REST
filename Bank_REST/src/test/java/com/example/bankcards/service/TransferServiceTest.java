package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.entity.*;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepo;
    @Mock
    private TransferRepository transferRepo;

    @InjectMocks
    private TransferService transferService;

    private User owner;
    private Card cardFrom;
    private Card cardTo;

    @BeforeEach
    void initData() {
        owner = new User();
        owner.setId(1L);

        cardFrom = new Card();
        cardFrom.setId(10L);
        cardFrom.setOwner(owner);
        cardFrom.setStatus(CardStatus.ACTIVE);
        cardFrom.setExpiryDate(LocalDate.now().plusYears(1));
        cardFrom.setBalance(BigDecimal.valueOf(200));

        cardTo = new Card();
        cardTo.setId(20L);
        cardTo.setOwner(owner);
        cardTo.setStatus(CardStatus.ACTIVE);
        cardTo.setExpiryDate(LocalDate.now().plusYears(1));
        cardTo.setBalance(BigDecimal.valueOf(50));
    }

    @Test
    void transfer_ShouldMoveMoneyBetweenCards() {
        when(cardRepo.findByIdAndOwner_IdForUpdate(10L, 1L)).thenReturn(Optional.of(cardFrom));
        when(cardRepo.findByIdAndOwner_IdForUpdate(20L, 1L)).thenReturn(Optional.of(cardTo));

        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(20L);
        req.setAmount(BigDecimal.valueOf(100));
        req.setDescription("Test transfer");

        transferService.transfer(1L, req);

        assertEquals(BigDecimal.valueOf(100), cardFrom.getBalance());
        assertEquals(BigDecimal.valueOf(150), cardTo.getBalance());
        verify(transferRepo).save(any(Transfer.class));
    }

    @Test
    void transfer_ShouldThrowIfInsufficientFunds() {
        cardFrom.setBalance(BigDecimal.valueOf(10));
        when(cardRepo.findByIdAndOwner_IdForUpdate(10L, 1L)).thenReturn(Optional.of(cardFrom));
        when(cardRepo.findByIdAndOwner_IdForUpdate(20L, 1L)).thenReturn(Optional.of(cardTo));

        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(20L);
        req.setAmount(BigDecimal.valueOf(100));

        assertThrows(BusinessException.class, () -> transferService.transfer(1L, req));
    }

    @Test
    void transfer_ShouldThrowIfNotOwner() {
        User otherUser = new User();
        otherUser.setId(2L);
        cardTo.setOwner(otherUser);

        when(cardRepo.findByIdAndOwner_IdForUpdate(10L, 1L)).thenReturn(Optional.of(cardFrom));
        // In the real service, we call with userId=1, and the to card should not be found (because owner is 2)
        when(cardRepo.findByIdAndOwner_IdForUpdate(20L, 1L)).thenReturn(Optional.empty());

        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(20L);
        req.setAmount(BigDecimal.valueOf(50));

        assertThrows(com.example.bankcards.exception.NotFoundException.class, () -> transferService.transfer(1L, req));
    }
}
