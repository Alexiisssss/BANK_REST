package com.example.bankcards.service;

import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }


    @Transactional
    public Long createUser(String username, String rawPassword, User.Role role, boolean enabled) {
        if (repo.existsByUsername(username)) throw new BusinessException("Username already taken");
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(role);
        u.setEnabled(enabled);
        repo.save(u);
        return u.getId();
    }

    @Transactional
    public void changeRole(Long userId, User.Role role) {
        User u = repo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        u.setRole(role);
    }

    @Transactional
    public void setEnabled(Long userId, boolean enabled) {
        User u = repo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        u.setEnabled(!enabled ? false : true);
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Attempting to load user by username: {}", username);
        User u = repo.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User with username '{}' not found", username);
                    return new UsernameNotFoundException("User not found");
                });
        log.info("User '{}' found, role: {}", username, u.getRole());

        return new org.springframework.security.core.userdetails.User(
                u.getUsername(),
                u.getPasswordHash(),
                u.isEnabled(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))
        );
    }

    @Transactional
    public Long registerUser(String username, String rawPassword) {
        log.info("Attempting to register new user: {}", username);
        if (repo.existsByUsername(username)) {
            log.warn("Registration failed: Username '{}' is already taken", username);
            throw new BusinessException("Username already taken");
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(User.Role.USER);
        repo.save(u);
        log.info("User '{}' registered successfully with ID {}", username, u.getId());
        return u.getId();
    }
}
