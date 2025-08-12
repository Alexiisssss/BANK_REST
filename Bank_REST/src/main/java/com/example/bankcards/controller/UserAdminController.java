package com.example.bankcards.controller;

import com.example.bankcards.dto.user.UserBlockRequest;
import com.example.bankcards.dto.user.UserCreateRequest;
import com.example.bankcards.dto.user.UserRoleUpdateRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for managing application users.
 * Provides endpoints for creating users, changing roles, and blocking/unblocking accounts.
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private static final Logger log = LoggerFactory.getLogger(UserAdminController.class);

    private final UserService userService;
    private final UserRepository userRepository;

    public UserAdminController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    /**
     * Create a new user.
     *
     * @param req user creation request payload
     * @return ID of the created user
     */
    @PostMapping
    public Long create(@Valid @RequestBody UserCreateRequest req) {
        log.info("Admin is creating a new user with username: {} and role: {}", req.getUsername(), req.getRole());
        Long userId = userService.createUser(
                req.getUsername(),
                req.getPassword(),
                User.Role.valueOf(req.getRole()),
                true
        );
        log.info("User created successfully with ID: {}", userId);
        return userId;
    }

    /**
     * Change user role.
     *
     * @param id  user ID
     * @param req request containing new role
     */
    @PutMapping("/{id}/role")
    public void changeRole(@PathVariable Long id, @Valid @RequestBody UserRoleUpdateRequest req) {
        log.info("Admin is changing role for user ID {} to {}", id, req.getRole());
        userService.changeRole(id, User.Role.valueOf(req.getRole()));
        log.info("Role for user ID {} updated successfully", id);
    }

    /**
     * Block or unblock a user.
     *
     * @param id  user ID
     * @param req request indicating block/unblock action
     */
    @PutMapping("/{id}/block")
    public void block(@PathVariable Long id, @RequestBody UserBlockRequest req) {
        boolean enableStatus = !req.isDisabled();
        if (enableStatus) {
            log.warn("Admin is unblocking user ID {}", id);
        } else {
            log.warn("Admin is blocking user ID {}", id);
        }
        userService.setEnabled(id, enableStatus);
        log.info("User ID {} block status updated: {}", id, !enableStatus ? "BLOCKED" : "ACTIVE");
    }
}