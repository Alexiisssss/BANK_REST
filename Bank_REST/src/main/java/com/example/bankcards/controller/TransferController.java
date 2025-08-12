package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService transferService;
    private final UserRepository userRepository;

    public TransferController(TransferService transferService, UserRepository userRepository) {
        this.transferService = transferService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public TransferResponse transfer(Authentication auth, @Valid @RequestBody TransferRequest req) {
        String username = auth.getName();
        Long userId = userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new com.example.bankcards.exception.NotFoundException("User not found"));
        return transferService.transfer(userId, req);
    }
}
