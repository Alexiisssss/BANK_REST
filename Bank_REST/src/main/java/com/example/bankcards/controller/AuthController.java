package com.example.bankcards.controller;

import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.LoginResponse;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.service.JwtService;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(AuthenticationManager authManager, JwtService jwtService, UserService userService) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        Long id = userService.registerUser(req.getUsername(), req.getPassword());
        log.info("User registered: username='{}', id={}", req.getUsername(), id);
        return ResponseEntity.ok(Map.of("id", id, "username", req.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            log.debug("Login attempt for username='{}'", req.getUsername());

            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

            UserDetails principal = (UserDetails) auth.getPrincipal();

            List<String> roles = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            String token = jwtService.generate(
                    principal.getUsername(),
                    Map.of("roles", roles)
            );

            log.info("User authenticated: username='{}', roles={}, tokenPrefix='{}...'",
                    principal.getUsername(),
                    roles,
                    token.substring(0, Math.min(token.length(), 12)));

            return ResponseEntity.ok(new LoginResponse(token));
        } catch (BadCredentialsException e) {
            log.warn("Login failed (bad credentials) for username='{}'", req.getUsername());
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        } catch (DisabledException e) {
            log.warn("Login failed (user disabled) for username='{}'", req.getUsername());
            return ResponseEntity.status(403).body(Map.of("error", "User is disabled"));
        } catch (LockedException e) {
            log.warn("Login failed (account locked) for username='{}'", req.getUsername());
            return ResponseEntity.status(423).body(Map.of("error", "User account is locked"));
        } catch (Exception e) {
            log.error("Login failed (unexpected) for username='{}': {}", req.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Unexpected error"));
        }
    }
}
