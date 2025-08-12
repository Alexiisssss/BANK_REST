package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferServiceConcurrencyTest {

    private final TransferService transferService;
    private final CardRepository cardRepo;
    private final UserRepository userRepo;

    private Long ownerId;
    private Long fromId;
    private Long toId;

    @Autowired
    public TransferServiceConcurrencyTest(TransferService transferService, CardRepository cardRepo, UserRepository userRepo) {
        this.transferService = transferService;
        this.cardRepo = cardRepo;
        this.userRepo = userRepo;
    }

    @BeforeEach
    void init() {
        // Create and save test user
        User owner = new User();
        owner.setUsername("test_user");
        owner.setPasswordHash("{noop}pwd");
        owner.setRole(User.Role.USER);
        owner.setEnabled(true);
        owner = userRepo.save(owner);
        ownerId = owner.getId();

        // Create source card with initial balance
        Card cardFrom = new Card();
        cardFrom.setOwner(owner);
        cardFrom.setEncryptedPan("enc-1111");
        cardFrom.setPanMask("**** **** **** 1111");
        cardFrom.setHolderName("Test User");
        cardFrom.setExpiryDate(LocalDate.now().plusYears(1));
        cardFrom.setStatus(CardStatus.ACTIVE);
        cardFrom.setBalance(BigDecimal.valueOf(200));

        // Create destination card with zero balance
        Card cardTo = new Card();
        cardTo.setOwner(owner);
        cardTo.setEncryptedPan("enc-2222");
        cardTo.setPanMask("**** **** **** 2222");
        cardTo.setHolderName("Test User");
        cardTo.setExpiryDate(LocalDate.now().plusYears(1));
        cardTo.setStatus(CardStatus.ACTIVE);
        cardTo.setBalance(BigDecimal.ZERO);

        cardFrom = cardRepo.save(cardFrom);
        cardTo = cardRepo.save(cardTo);

        fromId = cardFrom.getId();
        toId = cardTo.getId();
    }

    @Test
    void concurrentTransfers_ShouldKeepBalanceConsistent() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // Prepare transfer requests
        TransferRequest req150 = new TransferRequest();
        req150.setFromCardId(fromId);
        req150.setToCardId(toId);
        req150.setAmount(BigDecimal.valueOf(150));
        req150.setDescription("T1");

        TransferRequest req100 = new TransferRequest();
        req100.setFromCardId(fromId);
        req100.setToCardId(toId);
        req100.setAmount(BigDecimal.valueOf(100));
        req100.setDescription("T2");

        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> c1 = () -> {
            await(start);
            try {
                transferService.transfer(ownerId, req150);
                return true;
            } catch (BusinessException | ObjectOptimisticLockingFailureException | StaleObjectStateException e) {
                return false; // loser thread
            }
        };
        Callable<Boolean> c2 = () -> {
            await(start);
            try {
                transferService.transfer(ownerId, req100);
                return true;
            } catch (BusinessException | ObjectOptimisticLockingFailureException | StaleObjectStateException e) {
                return false;
            }
        };

        // Execute both transfers concurrently
        Future<Boolean> f1 = pool.submit(c1);
        Future<Boolean> f2 = pool.submit(c2);
        start.countDown();

        boolean s1 = f1.get();
        boolean s2 = f2.get();

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        Card from = cardRepo.findById(fromId).orElseThrow();
        Card to = cardRepo.findById(toId).orElseThrow();

        // Check invariant: total balance remains constant
        assertEquals(0, from.getBalance().add(to.getBalance()).compareTo(BigDecimal.valueOf(200)));

        // Exactly one transfer should succeed
        assertTrue(s1 ^ s2, "Exactly one thread should complete the transfer successfully");

        // Final allowed states:
        // - If 150 succeeded: from=50, to=150
        // - If 100 succeeded: from=100, to=100
        boolean case150 = from.getBalance().compareTo(BigDecimal.valueOf(50)) == 0
                && to.getBalance().compareTo(BigDecimal.valueOf(150)) == 0;
        boolean case100 = from.getBalance().compareTo(BigDecimal.valueOf(100)) == 0
                && to.getBalance().compareTo(BigDecimal.valueOf(100)) == 0;
        assertTrue(case150 || case100, "Unexpected final balance combination");
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }
    }
}
