package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.entity.*;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.MaskUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private CryptoService crypto;

    @InjectMocks
    private CardService cardService;

    @Test
    void create_ShouldSaveCardWithMaskedPan() {
        User user = new User();
        user.setId(1L);
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(crypto.encrypt("1234567812345678")).thenReturn("encryptedPan");

        CardCreateRequest req = new CardCreateRequest();
        req.setOwnerId(1L);
        req.setPan("1234567812345678");
        req.setHolderName("Aleks Vol");
        req.setExpiryDate(LocalDate.now().plusYears(1));
        req.setInitialBalance(BigDecimal.valueOf(100));

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);

        cardService.create(req);

        verify(cardRepo).save(captor.capture());
        Card saved = captor.getValue();
        assertEquals("encryptedPan", saved.getEncryptedPan());
        assertEquals(MaskUtils.maskPan("1234567812345678"), saved.getPanMask());
        assertEquals(BigDecimal.valueOf(100), saved.getBalance());
    }

    @Test
    void create_ShouldThrowIfExpiryDateInPast() {
        User user = new User();
        user.setId(1L);
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        CardCreateRequest req = new CardCreateRequest();
        req.setOwnerId(1L);
        req.setPan("1234567812345678");
        req.setHolderName("Aleks Vol");
        req.setExpiryDate(LocalDate.now().minusDays(1));

        assertThrows(BusinessException.class, () -> cardService.create(req));
    }
}